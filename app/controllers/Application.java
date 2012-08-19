package controllers;

import java.util.*;

import play.mvc.*;
import play.data.*;
import play.*;

import models.*;

public class Application extends Controller {

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
        word.translate();
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
        return badRequest("Not implemented yet");
    }

    public static Result saveRule() {
        return badRequest("Not implemented yet");
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
            rule.acceptChange();
        }
        for (Word word : Word.changedWords()) {
            word.acceptChange();
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

