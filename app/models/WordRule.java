package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.*;

@Entity 
public class WordRule extends Model {

    @Id
    public Long id;

    @Constraints.Required
    public Long word;

    @Constraints.Required
    public Long rule;

    @Constraints.Required
    public boolean applied;

    public static Finder<Long,WordRule> find = new Finder<Long,WordRule>(Long.class, WordRule.class);

    public static void resetWord(Word word) {
        for(WordRule wordRule : find.where().eq("word", word.id).findSet()) {
            wordRule.applied = false;
            wordRule.save();
        }
    }

    public static void applyRule(Word word, Rule rule) {
        WordRule wordRule = find.where().eq("word", word.id)
                                        .eq("rule", rule.id)
                                        .findUnique();
        if (wordRule == null) {
            wordRule = new WordRule();
            wordRule.word = word.id;
            wordRule.rule = rule.id;
        }
        wordRule.applied = true;
        wordRule.save();
    }
}