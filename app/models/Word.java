package models;

import java.util.*;
import javax.persistence.*;

import controllers.Application;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.*;

import org.liblouis.*;

@Entity 
public class Word extends Model implements Comparator<Rule> {

    @Id
    public Long id;
    
    @Constraints.Required
    public String text;

    @Constraints.Required
    public String expected;

    public String braille;

    public String prevBraille;

    public String origBraille;

    public Long getId() {
        return id;
    }

    public void init() {
        Set<Rule> disabledRules = Rule.find.where().eq("enabled", false).eq("newRule", false).findSet();
        Set<Rule> newRules = Rule.find.where().eq("enabled", true).eq("newRule", true).findSet();
        for (Rule rule : disabledRules) { rule.toggle(); }
        for (Rule rule : newRules) { rule.toggle(); }
        translate();
        for (Rule rule : disabledRules) { rule.toggle(); }
        for (Rule rule : newRules) { rule.toggle(); }
        translate();
        confirmChange();
    }

    public void translate() throws RuntimeException {
        if (prevBraille == null) { prevBraille = braille; }
        if (origBraille == null) { origBraille = braille; }
        TranslationResult result = null;
        boolean again = false;
        do {
            try {
                result = Application.liblouis.translate(text);
            } catch (Exception e) {
                throw new RuntimeException("Problem with liblouis", e);
            }
            braille = result.getBraille();
            WordRule.resetWord(this);
            again = false;
            for (TranslationRule nativeRule : result.getAppliedRules()) {
                if (nativeRule.getOpcode() < Opcodes.CTO_NONE) {
                    Rule rule = Rule.getRule(nativeRule);
                    if (!rule.enabled) {
                        nativeRule.toggle();
                        again = true;
                        break;
                    }
                    WordRule.applyRule(this, rule);
                }
            }
        } while(again);
        if (braille.equals(prevBraille)) { prevBraille = null; }
        if (braille.equals(origBraille)) { origBraille = null; }
        save();
    }

    public void confirmChange() {
        prevBraille = null;
        save();
    }

    public Iterator<Rule> getAppliedRules() {
        Set<Rule> rules = new TreeSet<Rule>(this);
        for (WordRule wordRule : WordRule.find.where().eq("word", this.id).eq("applied", true).findSet()) {
            rules.add(Rule.find.byId(wordRule.rule));
        }
        return rules.iterator();
    }

    public Iterator<Rule> getUnappliedRules() {
        Set<Rule> rules = new TreeSet<Rule>(this);
        for (WordRule wordRule : WordRule.find.where().eq("word", this.id).eq("applied", false).findSet()) {
            rules.add(Rule.find.byId(wordRule.rule));
        }
        return rules.iterator();
    }

    @Override
    public int compare(Rule rule1, Rule rule2) {
        return rule1.toString().compareTo(rule2.toString());
    }

    public static Finder<Long,Word> find = new Finder<Long,Word>(Long.class, Word.class);
   
    public static Set<Word> changedWords() {
        return find.where().isNotNull("prevBraille").findSet();
    }

    public static Page<Word> page(int page, int pageSize, String filter) {
        return 
            find.where()
                .ilike("text", "%" + filter + "%")
                .findPagingList(pageSize)
                .getPage(page);
    }
}

