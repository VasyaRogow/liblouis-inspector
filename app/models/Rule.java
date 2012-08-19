package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.*;

import org.liblouis.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

@Entity 
public class Rule extends Model {

    @Id
    public Long id;

    @Constraints.Required
    public String opcode;

    @Constraints.Required
    public String column1;

    @Constraints.Required
    public String column2;

    public boolean enabled;

    public boolean newRule;

    public boolean changed;

    public Long getId() {
        return id;
    }

    public String toString() {
        return opcode + " " + column1 + " " + column2;
    }
    
    public boolean toggle() {
        if (isReadOnly()) { return false; }
        enabled = !enabled;
        changed = !changed;
        save();
        TranslationRule rule = rulesMap.get(id);
        if (rule.isEnabled() != enabled) {
            rule.toggle();
        }
        return true;
    }

    public static List<String> opcodes = new ArrayList<String>();

    static {
        opcodes.add("word");
        opcodes.add("always");
        opcodes.add("begword");
        opcodes.add("endword");
        opcodes.add("midword");
        opcodes.add("midendword");
        opcodes.add("begmidword");
        opcodes.add("partword");
        opcodes.add("lowword");
        opcodes.add("sufword");
        opcodes.add("prfword");
    }

    public boolean isReadOnly() {
        return (!opcodes.contains(opcode) || !rulesMap.containsKey(id));
    }

    public void confirmChange() {
        changed = false;
        save();
    }

    private static final BiMap<Long,TranslationRule> rulesMap = HashBiMap.create();

    public static Finder<Long,Rule> find = new Finder<Long,Rule>(Long.class, Rule.class);

    public static Set<Rule> changedRules() {
        return find.where().eq("changed", true).findSet();
    }

    public static void clearCache() {
        rulesMap.clear();
    }

    public static Rule getRule(TranslationRule nativeRule) {
        
        // First search in cache
        Long ruleID = rulesMap.inverse().get(nativeRule);
        if (ruleID != null) {
            Rule rule = find.byId(ruleID);
            if (rule != null) {
                return rule;
            }
        }
        
        String op = Opcodes.getName(nativeRule.getOpcode());
        String col1 = null;
        String col2 = null;
        if (nativeRule.getChars() != null) {
            col1 = nativeRule.getChars().toString();
            col2 = nativeRule.getDots().toString();
        } else {
            MultipassScript script = nativeRule.getScript();
            col1 = script.getTest();
            col2 = script.getAction();
        }
        
        // Then search in database
        Rule rule = find.where().eq("opcode", op)
                                .eq("column1", col1)
                                .eq("column2", col2)
                                .findUnique();
        
        // Otherwise create a new rule
        if (rule == null) {
            rule = new Rule();
            rule.enabled = nativeRule.isEnabled();
            rule.opcode = op;
            rule.column1 = col1;
            rule.column2 = col2;
            rule.changed = false;
            rule.save();
        }
        
        // Put in cache
        rulesMap.put(rule.id, nativeRule);
        return rule;
    }
}

