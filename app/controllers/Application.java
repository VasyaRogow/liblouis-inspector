package controllers;

import java.io.File;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import play.mvc.*;
import play.data.*;
import play.*;

import models.*;

import com.sun.jna.Pointer;

import org.liblouis.*;
import org.liblouis.util.Environment;

public class Application extends Controller {

    public static Translator liblouis;
    private static File newTable;
    private static String liblouisTables;

    static {
        try {
            File tablePath = new File(
                    System.getProperty("java.io.tmpdir") + File.separator + "liblouis-inspector");
            if (!tablePath.isDirectory()) { tablePath.mkdir(); }
            Environment.setLouisTablePath(tablePath.getAbsolutePath());
            newTable = new File(tablePath.getAbsolutePath() + File.separator + "new_rules");
            if (!newTable.exists()) { newTable.createNewFile(); }
            liblouisTables = "en-US-g2.ctb" + "," + newTable.getName();
            writeNewRules();
            liblouis = new Translator(liblouisTables);
        } catch (Exception e) {
            throw new RuntimeException("Error during initialization", e);
        }
    }

    private static void writeNewRules() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(newTable));
            for (Rule rule : Rule.find.where().eq("newRule", true).findSet()) {
                writer.write(rule.toString());
                writer.newLine();
            }
            writer.close();
            Rule.clearCache();
            LouisLibrary.INSTANCE.lou_free();
        } catch (IOException e) {}
    }

    public static Result GO_HOME = redirect(
        routes.Application.listWords(0, "")
    );

    public static Result index() {
        return GO_HOME;
    }

    public static Long editingWord = 0L;

    public static Result backToEditingWord() {
        if (editingWord > 0) {
            return redirect(routes.Application.editWord(editingWord));
        } else {
            return GO_HOME;
        }
    }

    public static Result listWords(int page, String filter) {
        editingWord = 0L;
        if (Rule.changedRules().size() > 0) {
            return redirect(routes.Application.viewChanges());
        }
        return ok(
            views.html.listWords.render(
                Word.page(page, 10, filter), filter
            )
        );
    }

    public static Result newWord() {
        Form<Word> wordForm = form(Word.class);
        return ok(
            views.html.newWord.render(wordForm)
        );
    }

    public static Result saveWord() {
        Form<Word> wordForm = form(Word.class).bindFromRequest();
        if(wordForm.hasErrors()) {
            return badRequest(views.html.newWord.render(wordForm));
        }
        Word word = wordForm.get();
        word.save();
        word.init();
        flash("done", "Word " + word.text + " has been added");
        return editWord(word.id);
    }

    public static Result editWord(Long wordId) {
        editingWord = wordId;
        Word word = Word.find.byId(wordId);
        if (word == null) {
            return badRequest("Could not find word");
        }
        word.translate();
        return ok(views.html.editWord.render(word, word.getAppliedRules(), word.getUnappliedRules()));
    }

    public static Result toggleRule(Long ruleId) {
        Rule rule = Rule.find.byId(ruleId);
        if (rule != null) {
            rule.toggle();
            flash("done", "Rule " + rule + " has been " + (rule.enabled?"en":"dis") + "abled");
        }
        return backToEditingWord();
    }

    public static Result deleteWord(Long wordId) {
        Word word = Word.find.byId(wordId);
        if (word == null) {
            return badRequest("Could not find word");
        }
        for(WordRule wordRule : WordRule.find.where().eq("word", wordId).findSet()) {
            wordRule.delete();
        }
        word.delete();
        flash("done", "Word " + word.text + " has been deleted");
        return GO_HOME;
    }

    public static Result newRule() {
        Form<Rule> ruleForm = form(Rule.class);
        return ok(
            views.html.newRule.render(ruleForm)
        );
    }

    public static Result saveRule() {
        Form<Rule> ruleForm = form(Rule.class).bindFromRequest();
        if(ruleForm.hasErrors()) {
            return badRequest(views.html.newRule.render(ruleForm));
        }
        Rule rule = ruleForm.get();
        rule.enabled = true;
        rule.newRule = true;
        rule.changed = true;
        rule.save();
        writeNewRules();
        if (LouisLibrary.INSTANCE.lou_getTable(liblouisTables) == Pointer.NULL) {
            rule.delete();
            writeNewRules();
            flash("error", "Rule " + rule.toString() + " could not be compiled");
            return badRequest(views.html.newRule.render(ruleForm));
        }
        flash("done", "Rule " + rule.toString() + " has been added");
        return backToEditingWord();
    }

    public static Result viewChanges() {
        Set<Word> possiblyAffectedWords = new HashSet<Word>();
        for (Rule rule : Rule.changedRules()) {
            possiblyAffectedWords.addAll(Word.find.where().contains("text", rule.column1).findSet());
        }
        for (Word word : possiblyAffectedWords) {
            word.translate();
        }
        return ok(views.html.changes.render(Rule.changedRules(), Word.changedWords()));
    }

    public static Result confirmChanges() {
        for (Rule rule : Rule.changedRules()) {
            rule.confirmChange();
        }
        for (Word word : Word.changedWords()) {
            word.confirmChange();
        }
        return GO_HOME;
    }

    public static Result cancelChanges() {
        for (Rule rule : Rule.changedRules()) {
            rule.toggle();
        }
        for (Word word : Word.changedWords()) {
            word.translate();
        }
        return GO_HOME;
    }
}

