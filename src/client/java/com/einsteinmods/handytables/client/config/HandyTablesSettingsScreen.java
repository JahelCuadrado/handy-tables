package com.einsteinmods.handytables.client.config;

import com.einsteinmods.handytables.config.ClientSettingsFile;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Settings of {@code config/handytables.json}, built from vanilla widgets and laid out like a YACL
 * screen (as the other EinsteinMods): a tab bar, the options list on the left two thirds, and on
 * the right a description of the hovered option above the Cancel/Reset, Undo and Save/Done buttons.
 *
 * <p>Changes stay pending until "Save Changes", which writes the file and applies at once.
 */
public final class HandyTablesSettingsScreen extends Screen {
  private static final String KEY_PREFIX = "config.handytables.";
  private static final String DESCRIPTION_SUFFIX = ".description";
  private static final String TOOLTIP_SUFFIX = ".tooltip";

  private static final Component SAVE = Component.translatable(KEY_PREFIX + "save");
  private static final Tooltip SAVE_TOOLTIP = tooltip("save");
  private static final Tooltip DONE_TOOLTIP = tooltip("done");
  private static final Tooltip CANCEL_TOOLTIP = tooltip("cancel");
  private static final Component RESET_ALL = Component.translatable("controls.reset");
  private static final Tooltip RESET_ALL_TOOLTIP = tooltip("reset");
  private static final Component UNDO = Component.translatable(KEY_PREFIX + "undo");
  private static final Tooltip UNDO_TOOLTIP = tooltip("undo");

  /** Darker texture behind the description pane, as YACL. */
  private static final Identifier MENU_LIST_BACKGROUND =
      Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");

  /** Background and separator textures tile in squares of this size. */
  private static final int TEXTURE_SIZE = 32;

  private static final int TAB_HEADER_TEXTURE_SIZE = 16;

  private static final int SEPARATOR_THICKNESS = 2;
  private static final float VERTICAL_RADIANS = (float) Math.toRadians(90.0);

  /** The options list takes two of three columns; the right pane the third. */
  private static final int COLUMNS = 3;

  private static final int LIST_COLUMNS = 2;

  /** Pane padding is this fraction of the column width, as YACL. */
  private static final int PADDING_DIVISOR = 20;

  private static final int MAX_COLUMN_WIDTH = 400;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BUTTON_ROW_GAP = 2;
  private static final int BUTTON_GAP = 4;

  private final @Nullable Screen parent;
  private final TickBoxControl openSound;
  private final List<OptionControl<?>> controls;

  private final SettingsList list;
  private final OptionDescriptionPanel description;
  private final Button cancelOrReset;
  private final Button undo;
  private final Button saveOrDone;
  private final TabManager tabManager =
      new TabManager(this::addRenderableWidget, this::removeWidget);
  private final GeneralTab generalTab = new GeneralTab();
  private @Nullable MenuTabBar tabBar;
  private @Nullable ScreenRectangle descriptionArea;
  private @Nullable ScreenRectangle rightPane;

  public HandyTablesSettingsScreen(@Nullable Screen parent) {
    super(Component.translatable(KEY_PREFIX + "title"));
    this.parent = parent;
    openSound =
        tickBox(
            "open_sound",
            ClientSettings.get().playsOpenSound(),
            ClientSettingsFile.DEFAULT_OPEN_SOUND);
    controls = List.of(openSound);

    description = new OptionDescriptionPanel(font, openSound);
    list = new SettingsList(minecraft, description::show);
    list.addGroup(group("sounds"), groupDescription("sounds"));
    list.addOption(openSound);

    cancelOrReset = Button.builder(CommonComponents.GUI_CANCEL, button -> cancelOrReset()).build();
    undo = Button.builder(UNDO, button -> undoAll()).tooltip(UNDO_TOOLTIP).build();
    saveOrDone = Button.builder(CommonComponents.GUI_DONE, button -> saveOrDone()).build();
    updateButtons();
  }

  @Override
  protected void init() {
    MenuTabBar bar = MenuTabBar.builder(tabManager, width).addTabs(generalTab).build();
    tabBar = bar;
    addRenderableWidget(bar);
    bar.selectTab(0, false);
    repositionElements();
  }

  @Override
  protected void repositionElements() {
    if (tabBar == null) {
      return;
    }
    tabBar.arrangeElements(width);
    int top = tabBar.getRectangle().bottom();
    tabManager.setTabArea(new ScreenRectangle(0, top, width, height - top));
  }

  @Override
  public void onClose() {
    minecraft.gui.setScreen(parent);
  }

  /** Tab header texture above, menu background below, as vanilla tabbed screens. */
  @Override
  protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
    int headerHeight = tabBar == null ? 0 : tabBar.getRectangle().bottom();
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        CreateWorldScreen.TAB_HEADER_BACKGROUND,
        0,
        0,
        0.0F,
        0.0F,
        width,
        headerHeight,
        TAB_HEADER_TEXTURE_SIZE,
        TAB_HEADER_TEXTURE_SIZE);
    extractMenuBackground(graphics, 0, headerHeight, width, height - headerHeight);
  }

  /** Darker description pane with a separator along its left edge, as YACL. */
  @Override
  public void extractBackground(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractBackground(graphics, mouseX, mouseY, partialTick);
    if (rightPane == null) {
      return;
    }
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        minecraft.level == null
            ? MENU_LIST_BACKGROUND
            : AbstractSelectionList.INWORLD_MENU_LIST_BACKGROUND,
        rightPane.left(),
        rightPane.top(),
        rightPane.right(),
        rightPane.bottom(),
        rightPane.width(),
        rightPane.height(),
        TEXTURE_SIZE,
        TEXTURE_SIZE);
    graphics.pose().pushMatrix();
    graphics.pose().translate(rightPane.left(), rightPane.top());
    graphics.pose().rotate(VERTICAL_RADIANS);
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        minecraft.level == null ? FOOTER_SEPARATOR : INWORLD_FOOTER_SEPARATOR,
        0,
        0,
        0.0F,
        0.0F,
        rightPane.height(),
        SEPARATOR_THICKNESS,
        TEXTURE_SIZE,
        SEPARATOR_THICKNESS);
    graphics.pose().popMatrix();
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    if (descriptionArea != null) {
      description.extract(graphics, descriptionArea);
    }
  }

  private TickBoxControl tickBox(String key, boolean initial, boolean defaultValue) {
    return new TickBoxControl(
        font,
        Component.translatable(KEY_PREFIX + key),
        Component.translatable(KEY_PREFIX + key + DESCRIPTION_SUFFIX),
        initial,
        defaultValue,
        value -> updateButtons());
  }

  private static Component group(String key) {
    return Component.translatable(KEY_PREFIX + "group." + key);
  }

  private static Component groupDescription(String key) {
    return Component.translatable(KEY_PREFIX + "group." + key + DESCRIPTION_SUFFIX);
  }

  private static Tooltip tooltip(String button) {
    return Tooltip.create(Component.translatable(KEY_PREFIX + button + TOOLTIP_SUFFIX));
  }

  private boolean hasPendingChanges() {
    return controls.stream().anyMatch(OptionControl::isChanged);
  }

  /** Button labels follow YACL: Save Changes / Done, Cancel / Reset; Undo only with changes. */
  private void updateButtons() {
    boolean pending = hasPendingChanges();
    saveOrDone.setMessage(pending ? SAVE : CommonComponents.GUI_DONE);
    saveOrDone.setTooltip(pending ? SAVE_TOOLTIP : DONE_TOOLTIP);
    cancelOrReset.setMessage(pending ? CommonComponents.GUI_CANCEL : RESET_ALL);
    cancelOrReset.setTooltip(pending ? CANCEL_TOOLTIP : RESET_ALL_TOOLTIP);
    undo.active = pending;
  }

  private void saveOrDone() {
    if (!hasPendingChanges()) {
      onClose();
      return;
    }
    ClientSettings.save(ClientSettingsFile.of(openSound.get()));
    controls.forEach(OptionControl::markSaved);
    updateButtons();
  }

  private void cancelOrReset() {
    if (hasPendingChanges()) {
      onClose();
      return;
    }
    controls.forEach(OptionControl::resetQuietly);
    updateButtons();
  }

  private void undoAll() {
    controls.forEach(OptionControl::undo);
    updateButtons();
  }

  /**
   * The single category. Lays out YACL's columns: list on the left two thirds; on the right, the
   * description above Cancel/Reset + Undo, with Save/Done along the bottom.
   */
  private final class GeneralTab implements Tab {
    private final FrameLayout bounds = new FrameLayout();

    @Override
    public Component getTabTitle() {
      return Component.translatable(KEY_PREFIX + "category.general");
    }

    @Override
    public Component getTabExtraNarration() {
      return Component.empty();
    }

    @Override
    public void visitChildren(Consumer<AbstractWidget> childrenConsumer) {
      childrenConsumer.accept(list);
      childrenConsumer.accept(cancelOrReset);
      childrenConsumer.accept(undo);
      childrenConsumer.accept(saveOrDone);
    }

    @Override
    public void doLayout(ScreenRectangle area) {
      bounds.setPosition(area.left(), area.top());
      bounds.setMinDimensions(area.width(), area.height());
      bounds.arrangeElements();
      int column = area.width() / COLUMNS;
      int padding = column / PADDING_DIVISOR;
      int paneWidth = Math.min(column, MAX_COLUMN_WIDTH) - padding * 2;
      int listWidth = column * LIST_COLUMNS;
      int paneCenter = listWidth + column / 2;
      int paneLeft = paneCenter - paneWidth / 2;

      list.updateSizeAndPosition(listWidth + 1, area.height(), area.left(), area.top());
      rightPane =
          new ScreenRectangle(
              area.left() + listWidth + 1, area.top(), area.width() - listWidth - 1, area.height());

      int saveY = area.bottom() - padding - BUTTON_HEIGHT;
      saveOrDone.setRectangle(paneWidth, BUTTON_HEIGHT, paneLeft, saveY);
      int halfWidth = paneWidth / 2 - BUTTON_GAP / 2;
      int topRowY = saveY - BUTTON_HEIGHT - BUTTON_ROW_GAP;
      cancelOrReset.setRectangle(halfWidth, BUTTON_HEIGHT, paneLeft, topRowY);
      undo.setRectangle(halfWidth, BUTTON_HEIGHT, paneLeft + halfWidth + BUTTON_GAP, topRowY);

      int descriptionTop = area.top() + padding;
      descriptionArea =
          new ScreenRectangle(
              paneLeft, descriptionTop, paneWidth, Math.max(0, topRowY - padding - descriptionTop));
    }

    @Override
    public Layout getLayout() {
      return bounds;
    }
  }
}
