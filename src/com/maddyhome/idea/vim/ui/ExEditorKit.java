package com.maddyhome.idea.vim.ui;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.DigraphSequence;
import com.maddyhome.idea.vim.helper.SearchHelper;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 */
public class ExEditorKit extends DefaultEditorKit {
  public static ExEditorKit getInstance() {
    if (instance == null) {
      instance = new ExEditorKit();
    }

    return instance;
  }

  /**
   * Gets the MIME type of the data that this
   * kit represents support for.
   *
   * @return the type
   */
  public String getContentType() {
    return "text/ideavim";
  }

  /**
   * Fetches the set of commands that can be used
   * on a text component that is using a model and
   * view produced by this kit.
   *
   * @return the set of actions
   */
  public Action[] getActions() {
    Action[] res = TextAction.augmentList(super.getActions(), this.exActions);
    if (logger.isDebugEnabled()) logger.debug("res.length=" + res.length);

    return res;
  }

  /**
   * Creates an uninitialized text storage model
   * that is appropriate for this type of editor.
   *
   * @return the model
   */
  public Document createDefaultDocument() {
    return new ExDocument();
  }

  public static KeyStroke convert(ActionEvent event) {
    String cmd = event.getActionCommand();
    int mods = event.getModifiers();
    if (cmd != null && cmd.length() > 0) {
      char ch = cmd.charAt(0);
      if (ch < ' ') {
        if (mods == KeyEvent.CTRL_MASK) {
          return KeyStroke.getKeyStroke(KeyEvent.VK_A + ch - 1, mods);
        }
      }
      else {
        return KeyStroke.getKeyStroke(new Character(ch), mods);
      }
    }

    return null;
  }

  public static final String DefaultExKey = "default-ex-key";
  public static final String CancelEntry = "cancel-entry";
  public static final String CompleteEntry = "complete-entry";
  public static final String EscapeChar = "escape";
  public static final String DeletePreviousChar = "delete-prev-char";
  public static final String DeletePreviousWord = "delete-prev-word";
  public static final String DeleteToCursor = "delete-to-cursor";
  public static final String DeleteFromCursor = "delete-from-cursor";
  public static final String ToggleInsertReplace = "toggle-insert";
  public static final String InsertRegister = "insert-register";
  public static final String InsertWord = "insert-word";
  public static final String InsertWORD = "insert-WORD";
  public static final String HistoryUp = "history-up";
  public static final String HistoryDown = "history-down";
  public static final String HistoryUpFilter = "history-up-filter";
  public static final String HistoryDownFilter = "history-down-filter";
  public static final String StartDigraph = "start-digraph";

  protected Action[] exActions = new Action[]{
    new ExEditorKit.CancelEntryAction(),
    new ExEditorKit.CompleteEntryAction(),
    new ExEditorKit.EscapeCharAction(),
    new ExEditorKit.DeletePreviousCharAction(),
    new ExEditorKit.DeletePreviousWordAction(),
    new ExEditorKit.DeleteToCursorAction(),
    new ExEditorKit.DeleteFromCursorAction(),
    new ExEditorKit.HistoryUpAction(),
    new ExEditorKit.HistoryDownAction(),
    new ExEditorKit.HistoryUpFilterAction(),
    new ExEditorKit.HistoryDownFilterAction(),
    new ExEditorKit.ToggleInsertReplaceAction(),
    new ExEditorKit.StartDigraphAction()
  };

  public static class DefaultExKeyHandler extends DefaultKeyTypedAction {
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      KeyStroke key = convert(e);
      if (key != null) {
        char ch = target.checkKey(convert(e));
        if (ch > 0) {
          ActionEvent event = new ActionEvent(e.getSource(), e.getID(), "" + ch, e.getWhen(), e.getModifiers());
          super.actionPerformed(event);

          target.saveLastEntry();
        }
      }
      else {
        super.actionPerformed(e);

        target.saveLastEntry();
      }
    }
  }

  public static class HistoryUpAction extends TextAction {
    public HistoryUpAction() {
      super(HistoryUp);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(true, false);
    }
  }

  public static class HistoryDownAction extends TextAction {
    public HistoryDownAction() {
      super(HistoryDown);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(false, false);
    }
  }

  public static class HistoryUpFilterAction extends TextAction {
    public HistoryUpFilterAction() {
      super(HistoryUpFilter);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(true, true);
    }
  }

  public static class HistoryDownFilterAction extends TextAction {
    public HistoryDownFilterAction() {
      super(HistoryDownFilter);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(false, true);
    }
  }

  // TODO - how do I get the argument (register name)?
  public static class InsertRegisterAction extends TextAction {
    public InsertRegisterAction() {
      super(InsertRegister);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();
    }
  }

  public static class CompleteEntryAction extends TextAction {
    public CompleteEntryAction() {
      super(CompleteEntry);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      logger.debug("complete entry");
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

      KeyHandler.getInstance().handleKey(
        ExEntryPanel.getInstance().getEntry().getEditor(),
        stroke,
        ExEntryPanel.getInstance().getEntry().getContext());
    }
  }

  public static class CancelEntryAction extends TextAction {
    public CancelEntryAction() {
      super(CancelEntry);
    }

    public void actionPerformed(ActionEvent e) {
      CommandGroups.getInstance().getProcess().cancelExEntry(
        ExEntryPanel.getInstance().getEntry().getEditor(),
        ExEntryPanel.getInstance().getEntry().getContext());
    }
  }

  public static class EscapeCharAction extends TextAction {
    public EscapeCharAction() {
      super(EscapeChar);
    }

    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.escape();
    }
  }

  public static class DeletePreviousCharAction extends TextAction {
    public DeletePreviousCharAction() {
      super(DeletePreviousChar);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      try {
        Document doc = target.getDocument();
        Caret caret = target.getCaret();
        int dot = caret.getDot();
        int mark = caret.getMark();
        if (dot != mark) {
          doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
        }
        else if (dot > 0) {
          int delChars = 1;

          if (dot > 1) {
            String dotChars = doc.getText(dot - 2, 2);
            char c0 = dotChars.charAt(0);
            char c1 = dotChars.charAt(1);

            if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
                c1 >= '\uDC00' && c1 <= '\uDFFF') {
              delChars = 2;
            }
          }

          doc.remove(dot - delChars, delChars);
        }
        else {
          CommandGroups.getInstance().getProcess().cancelExEntry(
            ExEntryPanel.getInstance().getEntry().getEditor(),
            ExEntryPanel.getInstance().getEntry().getContext());
        }
      }
      catch (BadLocationException bl) {
        // ignore
      }
    }
  }

  public static class DeletePreviousWordAction extends TextAction {
    public DeletePreviousWordAction() {
      super(DeletePreviousWord);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      Document doc = target.getDocument();
      Caret caret = target.getCaret();
      int offset = SearchHelper.findNextWord(target.getText(), caret.getDot(), target.getText().length(),
                                             -1, false, false);
      if (logger.isDebugEnabled()) logger.debug("offset=" + offset);
      try {
        int pos = caret.getDot();
        doc.remove(offset, pos - offset);
      }
      catch (BadLocationException ex) {
        // ignore
      }
    }
  }

  public static class DeleteToCursorAction extends TextAction {
    public DeleteToCursorAction() {
      super(DeleteToCursor);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      Document doc = target.getDocument();
      Caret caret = target.getCaret();
      try {
        doc.remove(0, caret.getDot());
      }
      catch (BadLocationException ex) {
        // ignore
      }
    }
  }

  public static class DeleteFromCursorAction extends TextAction {
    public DeleteFromCursorAction() {
      super(DeleteFromCursor);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      Document doc = target.getDocument();
      Caret caret = target.getCaret();
      try {
        doc.remove(caret.getDot(), doc.getLength());
      }
      catch (BadLocationException ex) {
        // ignore
      }
    }
  }

  public static class ToggleInsertReplaceAction extends TextAction {
    public ToggleInsertReplaceAction() {
      super(ToggleInsertReplace);

      logger.debug("ToggleInsertReplaceAction()");
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      logger.debug("actionPerformed");
      ExTextField target = (ExTextField)getTextComponent(e);
      target.toggleInsertReplace();
    }
  }

  public static class StartDigraphAction extends TextAction {
    public StartDigraphAction() {
      super(StartDigraph);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      KeyStroke key = convert(actionEvent);
      if (key != null && DigraphSequence.isDigraphStart(key)) {
        target.startDigraph(convert(actionEvent));
      }
    }
  }

  private static ExEditorKit instance;

  private static final Logger logger = Logger.getInstance(ExEditorKit.class.getName());
}
