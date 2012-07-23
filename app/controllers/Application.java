package controllers;

import java.util.*;

import play.mvc.*;
import play.data.*;
import play.*;

import models.*;

public class Application extends Controller {
    
    public static Result GO_HOME = redirect(
        routes.Application.listWords(0, "text", "asc", "")
    );
    
    public static Result index() {
        return GO_HOME;
    }

    public static Result listWords(int page, String sortBy, String order, String filter) {     
        return ok(
            views.html.listWords.render(
                Word.page(page, 10, sortBy, order, filter),
                sortBy, order, filter
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
        word.translate();
        word.save();
        flash("success", "Word " + wordForm.get().text + " has been added");
        return GO_HOME;
    }

    public static Result editWord(Long id) {
        Word word = Word.find.byId(id);
        if (word == null) {
            return badRequest("Could not find word");
        }
        word.translate();
        word.save();
        return ok(views.html.editWord.render(word));
    }

    public static Result toggleRule(Long id) {
        Rule rule = Rule.find.byId(Long.parseLong(request().body().asFormUrlEncoded().get("id")[0]));
        if (rule != null) {
            rule.toggle();
            rule.save();
            for (Word word : Word.find.where().contains("text", rule.column1).findSet()) {
	            word.translate();
	            word.save();
	        }
            flash("success", "Rule " + rule + " has been " + (rule.enabled?"en":"dis") + "abled");
        }
        return editWord(id);
    }
}
            
