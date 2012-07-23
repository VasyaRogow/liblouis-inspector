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
    public boolean enabled;

    @Constraints.Required
    public String opcode;

    @Constraints.Required
    public String column1;

    @Constraints.Required
    public String column2;

    public Long getId() {
        return id;
    }
    
    public String toString() {
        return opcode + " " + column1 + " " + column2;
    }
    
    public void toggle() {
        enabled = !enabled;
        TranslationRule rule = rulesMap.get(id);
        if (rule != null && rule.isEnabled() != enabled) {
            rule.toggle();
        }
    }

    public boolean isReadOnly() {
        if (!opcode.equals("word") &&
            !opcode.equals("always") &&
            !opcode.equals("begword") &&
            !opcode.equals("endword") &&
            !opcode.equals("midword")) {
            return true;
        }
        return !rulesMap.containsKey(id);
    }

    private static final BiMap<Long,TranslationRule> rulesMap = HashBiMap.create();

    public static Finder<Long,Rule> find = new Finder<Long,Rule>(Long.class, Rule.class);

    public static Rule getInstance(TranslationRule rule) {
        
        // First search in cache
        Rule instance = null;
        Long ruleID = rulesMap.inverse().get(rule);
        if (ruleID != null) {
            instance = find.byId(ruleID);
            if (instance != null) {
                return instance;
            }
        }
        
        String op = Opcodes.getName(rule.getOpcode());
        String col1 = null;
        String col2 = null;
        if (rule.getChars() != null) {
            col1 = rule.getChars().toString();
            col2 = rule.getDots().toString();
        } else {
            MultipassScript script = rule.getScript();
            col1 = script.getTest();
            col2 = script.getAction();
        }
        
        // Then search in database
        instance = find.where().like("opcode", op)
                               .like("column1", col1)
                               .like("column2", col2)
                               .findUnique();
        
        // Otherwise create a new rule
        if (instance == null) {
            instance = new Rule();
            instance.enabled = rule.isEnabled();
            instance.opcode = op;
            instance.column1 = col1;
            instance.column2 = col2;
            instance.save();
        }
        
        // Put in cache
        rulesMap.put(instance.getId(), rule);
        return instance;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public boolean equals(Object o) {
        try {
            Rule that = (Rule)o;
            return that.id.equals(this.id);
        } catch (ClassCastException e) {
            return false;
        }
    }
}

