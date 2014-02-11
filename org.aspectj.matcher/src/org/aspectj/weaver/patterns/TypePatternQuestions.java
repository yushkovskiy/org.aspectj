/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 * ******************************************************************/


package org.aspectj.weaver.patterns;

import org.aspectj.util.FuzzyBoolean;
import org.aspectj.weaver.ResolvedType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class TypePatternQuestions {
  private final Map questionsAndAnswers = new HashMap();

  public FuzzyBoolean askQuestion(TypePattern pattern, ResolvedType type,
                                  TypePattern.MatchKind kind) {
    final Question question = new Question(pattern, type, kind);
    //??? should we use this table to do caching or is that a pessimization
    //??? if we do that optimization we can also do error checking that the result
    //??? doesn't change
    final FuzzyBoolean answer = question.ask();
    questionsAndAnswers.put(question, answer);
    return answer;
  }

  public Question anyChanges() {
    for (final Iterator i = questionsAndAnswers.entrySet().iterator(); i.hasNext(); ) {
      final Map.Entry entry = (Map.Entry) i.next();
      final Question question = (Question) entry.getKey();
      final FuzzyBoolean expectedAnswer = (FuzzyBoolean) entry.getValue();

      final FuzzyBoolean currentAnswer = question.ask();
      //System.out.println(question + ":" + currentAnswer);
      if (currentAnswer != expectedAnswer) {
        return question;
      }
    }

    return null;
  }

  public String toString() {
    final StringBuffer buf = new StringBuffer();
    buf.append("TypePatternQuestions{");
    for (final Iterator i = questionsAndAnswers.entrySet().iterator(); i.hasNext(); ) {
      final Map.Entry entry = (Map.Entry) i.next();
      final Question question = (Question) entry.getKey();
      final FuzzyBoolean expectedAnswer = (FuzzyBoolean) entry.getValue();
      buf.append(question);
      buf.append(":");
      buf.append(expectedAnswer);
      buf.append(", ");
    }
    buf.append("}");
    return buf.toString();
  }


  public class Question {
    TypePattern pattern;
    ResolvedType type;
    TypePattern.MatchKind kind;

    public Question(TypePattern pattern, ResolvedType type,
                    TypePattern.MatchKind kind) {
      super();
      this.pattern = pattern;
      this.type = type;
      this.kind = kind;
    }

    public FuzzyBoolean ask() {
      return pattern.matches(type, kind);
    }

    public boolean equals(Object other) {
      if (!(other instanceof Question)) return false;
      final Question o = (Question) other;
      return o.pattern.equals(pattern) && o.type.equals(type) && o.kind == kind;
    }

    public int hashCode() {
      int result = 17;
      result = 37 * result + kind.hashCode();
      result = 37 * result + pattern.hashCode();
      result = 37 * result + type.hashCode();
      return result;
    }

    public String toString() {
      return "?(" + pattern + ", " + type + ", " + kind + ")";
    }
  }
}
