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
    
    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name="word_appliedrule")
    public Set<Rule> appliedRules = new HashSet<Rule>();
    
    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name="word_unappliedrule")
    public Set<Rule> unappliedRules = new HashSet<Rule>();

    public void translate() {
        try {
            for (Rule rule : unappliedRules) { rule.refresh(); }
            for (Rule rule : appliedRules) { rule.refresh(); unappliedRules.add(rule); }
            boolean again = false;
            do {
                again = false;
                TranslationResult result = liblouis.translate(text);
                braille = result.getBraille();
                appliedRules.clear();
                for (TranslationRule rule : result.getAppliedRules()) {
                    if (rule.getOpcode() < Opcodes.CTO_NONE) {
                        Rule instance = Rule.getInstance(rule);
                        if (!instance.enabled) {
                            rule.toggle();
                            again = true;
                            break;
                        }
                        appliedRules.add(instance);
                    }
                }
            } while(again);
            for (Rule rule : appliedRules) { unappliedRules.remove(rule); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Iterator<Rule> getSortedAppliedRules() {
        Set<Rule> sortedRules = new TreeSet<Rule>(this);
        for (Rule rule : appliedRules) { rule.refresh(); sortedRules.add(rule); }
        return sortedRules.iterator();        
    }

    public Iterator<Rule> getSortedUnappliedRules() {
        Set<Rule> sortedRules = new TreeSet<Rule>(this);
        for (Rule rule : unappliedRules) { rule.refresh(); sortedRules.add(rule); }
        return sortedRules.iterator();        
    }

    @Override
    public int compare(Rule rule1, Rule rule2) {
        return rule1.toString().compareTo(rule2.toString());
    }

    private static final Translator liblouis = new Translator("en-US-g2.ctb");

    public static Finder<Long,Word> find = new Finder<Long,Word>(Long.class, Word.class);
    
    public static Page<Word> page(int page, int pageSize, String sortBy, String order, String filter) {
        return 
            find.where()
                .ilike("text", "%" + filter + "%")
                .orderBy(sortBy + " " + order)
                .findPagingList(pageSize)
                .getPage(page);
    }
}

