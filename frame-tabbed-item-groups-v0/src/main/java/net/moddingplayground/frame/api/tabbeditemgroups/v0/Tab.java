package net.moddingplayground.frame.api.tabbeditemgroups.v0;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.moddingplayground.frame.api.util.GUIIcon;

import java.util.List;
import java.util.function.Function;

import static net.moddingplayground.frame.api.util.FrameUtil.*;

public class Tab {
    public static final Identifier DEFAULT_TAB_BACKGROUND = new Identifier("frame", "textures/tabbed_item_group/tab_background");
    public static final int MAX_RECOMMENDED_TABS = TabWidget.TABS_PER_COLUMN * 2;

    private final String id;
    private final GUIIcon<?> icon;
    private final Predicate predicate;
    private final GUIIcon<Identifier> backgroundTexture;
    private final Function<Tab, Text> displayText;

    private int index;
    private TabbedItemGroup group;

    protected Tab(String id, GUIIcon<?> icon, Predicate predicate, GUIIcon<Identifier> backgroundTexture, Function<Tab, Text> displayText) {
        this.id = id;
        this.icon = icon;
        this.predicate = predicate;
        this.backgroundTexture = backgroundTexture;
        this.displayText = displayText;

        this.index = -1;
        this.group = null;
    }

    public String getId() {
        return this.id;
    }

    public GUIIcon<?> getIcon() {
        return this.icon;
    }

    public Predicate getPredicate() {
        return this.predicate;
    }

    public GUIIcon<Identifier> getBackgroundTexture() {
        return this.backgroundTexture;
    }

    public int getIndex() {
        return this.index;
    }

    public TabbedItemGroup getGroup() {
        return this.group;
    }

    public Text getDisplayText() {
        return this.displayText.apply(this);
    }

    public boolean addToGroup(TabbedItemGroup group) {
        if (this.group != null) return false;
        this.group = group;
        this.index = group.getTabs().indexOf(this);
        return true;
    }

    public static Text createDisplayText(ItemGroup group, Tab tab) {
        TranslatableText text = (TranslatableText) group.getDisplayName();
        return new TranslatableText("%s.tab.%s".formatted(text.getKey(), tab.getId()));
    }

    public static Builder builder() {
        return new Builder();
    }

    @FunctionalInterface
    public interface Predicate {
        boolean test(TabbedItemGroup group, Item item);

        Predicate ALWAYS = (group, item) -> true;
        Predicate NEVER = (group, item) -> false;
        Predicate CONTAINS = (group, item) -> item.getGroup() == group;

        static Predicate tag(TagKey<Item> tag) {
            return (group, item) -> item.getDefaultStack().isIn(tag);
        }

        static Predicate items(ItemConvertible... items) {
            List<ItemConvertible> list = List.of(items);
            return (group, item) -> list.contains(item);
        }
    }

    public static class Builder {
        private Predicate predicate = Predicate.CONTAINS;
        private GUIIcon<Identifier> backgroundTexture = iconOf(DEFAULT_TAB_BACKGROUND);
        private Function<Tab, Text> displayText = Util.memoize(tab -> createDisplayText(tab.getGroup(), tab));

        protected Builder() {}

        public Builder predicate(Predicate predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder backgroundTexture(GUIIcon<Identifier> backgroundTexture) {
            this.backgroundTexture = backgroundTexture;
            return this;
        }

        public Builder displayText(Function<Tab, Text> displayText) {
            this.displayText = displayText;
            return this;
        }

        public Tab build(String id, GUIIcon<?> icon) {
            return new Tab(id, icon, this.predicate, this.backgroundTexture, this.displayText);
        }
    }
}
