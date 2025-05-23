package org.embeddedt.embeddium.impl.gui.frame.components;

import com.google.common.base.Predicates;
import net.minecraft.util.StringUtil;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen;
import org.embeddedt.embeddium.impl.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchTextFieldModel {
    boolean selecting;
    String text = "";
    int maxLength = 100;
    boolean visible = true;
    boolean editable = true;
    int firstCharacterIndex;
    int selectionStart;
    int selectionEnd;
    int lastCursorPosition = this.getCursor();
    Set<Option<?>> selectedOptions;
    final Set<Option<?>> allOptions;
    final Collection<OptionPage> pages;
    int innerWidth;
    EmbeddiumVideoOptionsScreen mainScreen;

    public SearchTextFieldModel(Collection<OptionPage> pages, EmbeddiumVideoOptionsScreen mainScreen) {
        this.pages = pages;
        this.allOptions = pages.stream().flatMap(p -> p.getOptions().stream()).collect(Collectors.toUnmodifiableSet());
        this.selectedOptions = this.allOptions;
        this.mainScreen = mainScreen;
    }

    int getMaxLength() {
        return this.maxLength;
    }

    public String getSelectedText() {
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        return this.text.substring(i, j);
    }

    public void write(String text) {


        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        int k = this.maxLength - this.text.length() - (i - j);
        //? if <1.20.6
        String string = SharedConstants.filterText(text);
        //? if >=1.20.6
        /*String string = StringUtil.filterText(text);*/
        int l = string.length();
        if (k < l) {
            string = string.substring(0, k);
            l = k;
        }

        String string2 = (new StringBuilder(this.text)).replace(i, j, string).toString();
        if (string2 != null) {
            this.text = string2;
            this.setSelectionStart(i + l);
            this.setSelectionEnd(this.selectionStart);
            this.onChanged(this.text);
        }
    }

    public Predicate<Option<?>> getOptionPredicate() {
        return selectedOptions == allOptions ? Predicates.alwaysTrue() : selectedOptions::contains;
    }

    private void onChanged(String newText) {
        selectedOptions = allOptions;
        if (this.editable) {
            if (!newText.trim().isEmpty()) {
                List<Option<?>> fuzzy = StringUtils.fuzzySearch(() -> this.pages.stream().flatMap(p -> p.getOptions().stream()).iterator(), newText, 2, o -> {
                    String name = o.getName().getString();
                    if(o.getControl() instanceof CyclingControl<?> cycler) {
                        name += " " + Arrays.stream(cycler.getNames()).map(Component::getString).collect(Collectors.joining(" "));
                    }
                    return name;
                });
                selectedOptions = new HashSet<>(fuzzy);
            }
        }

        //? if <1.21.2
        Minecraft.getInstance().tell(() -> this.mainScreen.rebuildUI());
        //? if >=1.21.2
        /*Minecraft.getInstance().schedule(() -> this.mainScreen.rebuildUI());*/
    }

    void erase(int offset) {
        if (Screen.hasControlDown()) {
            this.eraseWords(offset);
        } else {
            this.eraseCharacters(offset);
        }

    }

    public void eraseWords(int wordOffset) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.selectionStart) {
                this.write("");
            } else {
                this.eraseCharacters(this.getWordSkipPosition(wordOffset) - this.selectionStart);
            }
        }
    }

    public void eraseCharacters(int characterOffset) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.selectionStart) {
                this.write("");
            } else {
                int i = this.getCursorPosWithOffset(characterOffset);
                int j = Math.min(i, this.selectionStart);
                int k = Math.max(i, this.selectionStart);
                if (j != k) {
                    String string = (new StringBuilder(this.text)).delete(j, k).toString();
                    if (string != null) {
                        this.text = string;
                        this.setCursor(j);
                        this.onChanged(this.text);
                    }
                }
            }
        }
    }

    public int getWordSkipPosition(int wordOffset) {
        return this.getWordSkipPosition(wordOffset, this.getCursor());
    }

    private int getWordSkipPosition(int wordOffset, int cursorPosition) {
        return this.getWordSkipPosition(wordOffset, cursorPosition, true);
    }

    private int getWordSkipPosition(int wordOffset, int cursorPosition, boolean skipOverSpaces) {
        int i = cursorPosition;
        boolean bl = wordOffset < 0;
        int j = Math.abs(wordOffset);

        for (int k = 0; k < j; ++k) {
            if (!bl) {
                int l = this.text.length();
                i = this.text.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while (skipOverSpaces && i < l && this.text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while (skipOverSpaces && i > 0 && this.text.charAt(i - 1) == ' ') {
                    --i;
                }

                while (i > 0 && this.text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
    }

    public int getCursor() {
        return this.selectionStart;
    }

    public void setCursor(int cursor) {
        this.setSelectionStart(cursor);
        if (!this.selecting) {
            this.setSelectionEnd(this.selectionStart);
        }
    }

    public void moveCursor(int offset) {
        this.setCursor(this.getCursorPosWithOffset(offset));
    }

    private int getCursorPosWithOffset(int offset) {
        return this.text.offsetByCodePoints(this.selectionStart, offset);
    }

    public void setSelectionStart(int cursor) {
        this.selectionStart = Mth.clamp(cursor, 0, this.text.length());
    }

    public void setCursorToStart() {
        this.setCursor(0);
    }

    public void setCursorToEnd() {
        this.setCursor(this.text.length());
    }

    public void setSelectionEnd(int index) {
        int i = this.text.length();
        this.selectionEnd = Mth.clamp(index, 0, i);
        var textRenderer = Minecraft.getInstance().font;
        if (textRenderer != null) {
            if (this.firstCharacterIndex > i) {
                this.firstCharacterIndex = i;
            }

            int j = this.innerWidth;
            //? if <1.16 {
            /*String string = textRenderer.substrByWidth(this.text.substring(this.firstCharacterIndex), j);
            *///?} else
            String string = textRenderer.plainSubstrByWidth(this.text.substring(this.firstCharacterIndex), j);
            int k = string.length() + this.firstCharacterIndex;
            if (this.selectionEnd == this.firstCharacterIndex) {
                //? if <1.16 {
                /*this.firstCharacterIndex -= textRenderer.substrByWidth(this.text, j, true).length();
                *///?} else
                this.firstCharacterIndex -= textRenderer.plainSubstrByWidth(this.text, j, true).length();
            }

            if (this.selectionEnd > k) {
                this.firstCharacterIndex += this.selectionEnd - k;
            } else if (this.selectionEnd <= this.firstCharacterIndex) {
                this.firstCharacterIndex -= this.firstCharacterIndex - this.selectionEnd;
            }

            this.firstCharacterIndex = Mth.clamp(this.firstCharacterIndex, 0, i);
        }
    }
}
