package models;

import java.util.*;
import javax.persistence.*;

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

    public String oldBraille;

    public boolean changed;

    public Long getId() {
        return id;
    }

    public void translate() throws RuntimeException {
        TranslationResult result = null;
        boolean again = false;
        do {
            try {
                result = liblouis.translate(text);
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
        if (oldBraille == null) {
            oldBraille = braille;
        }
        changed = !oldBraille.equals(braille);
        save();
    }

    public void acceptChange() {
        oldBraille = braille;
        changed = false;
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

    private static final Translator liblouis = new Translator("en-US-g2.ctb");

    public static Finder<Long,Word> find = new Finder<Long,Word>(Long.class, Word.class);
   
    public static Set<Word> changedWords() {
        return find.where().eq("changed", true).findSet();
    }

    public static Page<Word> page(int page, int pageSize, String sortBy, String order, String filter) {
        return 
            find.where()
                .ilike("text", "%" + filter + "%")
                .orderBy(sortBy + " " + order)
                .findPagingList(pageSize)
                .getPage(page);
    }
}

