package com.sage.teachingassistant.workflow;

/**
 * Decides whether the user's message means "this stage is good, move on".
 *
 * <p>Kept behind an interface because this is a judgement call that will
 * eventually want a model behind it. The bundled implementation matches a fixed
 * list of phrases.
 */
public interface ApprovalClassifier {

    boolean isApproval(String message);
}
