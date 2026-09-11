package com.quzzar.kithkyn.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.genetics.AppearanceGenes;
import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A controlled visual comparison of every villager age stage.
 *
 * <p>All four preview people share the same appearance genes and default
 * client-side attributes. Only {@link AgeStage} changes, so a screenshot makes
 * model proportions and relative stage scale directly comparable. The same four
 * can be shown asleep: a client-side sleeping position shuts their eyes and rests
 * their heads without laying them down, which photographs the sleeping face at
 * every stage. The beds themselves are the world preview's job.
 *
 * <p>The undead lineup uses the same genes on the other {@link Kind}, dresses
 * the adult as a farmer so the rags read against a dark garment, and adds an
 * armed guard in leather at the end: the skeleton a player actually meets
 * first, and the one case where armour and a weapon have to sit right on bone.
 * Asleep, the undead keep their sockets: a skull has no lids to shut.
 */
public final class AgeLineupScreen extends Screen {

    static final int PREVIEW_SEED = 2_073_418;
    private static final int BACKGROUND_TOP = 0xFF20242A;
    private static final int BACKGROUND_BOTTOM = 0xFF15181C;
    private static final int GUIDE = 0xFF3A4048;
    private static final int BASELINE = 0xFFD5A94E;
    private static final int PRIMARY_TEXT = 0xFFF2F0EA;
    private static final int SECONDARY_TEXT = 0xFF9EA5AE;

    private final List<StagePreview> previews;
    private final Kind kind;
    private final boolean asleep;

    public AgeLineupScreen(ClientLevel level, Kind kind, boolean asleep) {
        super(Component.literal(title(kind, asleep)));
        this.kind = kind;
        this.asleep = asleep;
        this.previews = createPreviews(level, kind, asleep);
    }

    private static String title(Kind kind, boolean asleep) {
        String base = kind == Kind.UNDEAD ? "Undead age stages" : "Villager age stages";
        return asleep ? base + ", asleep" : base;
    }

    private static String subtitle(Kind kind, boolean asleep) {
        if (asleep) {
            return kind == Kind.UNDEAD
                    ? "The undead lineup asleep: heads at rest, and a skull has no lids to shut"
                    : "The waking lineup asleep: eyes shut, heads at rest";
        }
        return kind == Kind.UNDEAD
                ? "Same genes as the living lineup; only kind and age change"
                : "Same appearance and attributes; only age changes";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, BACKGROUND_TOP, BACKGROUND_BOTTOM);

        graphics.drawCenteredString(font, title, width / 2, 14, PRIMARY_TEXT);
        graphics.drawCenteredString(
                font,
                subtitle(kind, asleep),
                width / 2,
                27,
                SECONDARY_TEXT);

        int baselineY = height - 47;
        int columnWidth = width / previews.size();
        graphics.fill(14, baselineY, width - 14, baselineY + 1, BASELINE);

        int entityScale = Mth.clamp(width / 8, 52, 76);
        for (int index = 0; index < previews.size(); index++) {
            StagePreview preview = previews.get(index);
            int left = index * columnWidth;
            int right = index == previews.size() - 1 ? width : left + columnWidth;
            int centerX = (left + right) / 2;

            if (index > 0) {
                graphics.fill(left, 43, left + 1, baselineY - 8, GUIDE);
            }
            graphics.fill(centerX - 2, baselineY - 1, centerX + 3, baselineY + 2, BASELINE);

            graphics.enableScissor(left + 3, 40, right - 3, baselineY);
            renderPerson(graphics, centerX, baselineY, entityScale, preview.person());
            graphics.disableScissor();

            graphics.drawCenteredString(font, preview.label(), centerX, baselineY + 10, PRIMARY_TEXT);
            // Five columns leave no room for the long caption; say the same thing shorter.
            String detail = font.width(preview.detail()) <= columnWidth - 8 ? preview.detail() : preview.shortDetail();
            graphics.drawCenteredString(font, detail, centerX, baselineY + 22, SECONDARY_TEXT);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<StagePreview> createPreviews(ClientLevel level, Kind kind, boolean asleep) {
        List<StagePreview> created = new ArrayList<>();
        for (AgeStage stage : AgeStage.values()) {
            RealPerson person = previewPerson(level, kind, stage, asleep);
            // The undead adult farms: a dark garment, so rags read against bone,
            // where the wanderer's white shirt hid them.
            boolean farmer = kind == Kind.UNDEAD && stage == AgeStage.ADULT;
            if (farmer) {
                person.setOccupation(Occupation.FARMER);
            }
            created.add(new StagePreview(
                    stageName(stage),
                    farmer ? "adult proportions, farmer" : stage.usesYoungModel() ? "young proportions" : "adult proportions",
                    farmer ? "farmer" : stage.usesYoungModel() ? "young build" : "adult build",
                    person));
        }
        if (kind == Kind.UNDEAD) {
            RealPerson guard = previewPerson(level, kind, AgeStage.ADULT, asleep);
            guard.setOccupation(Occupation.GUARD);
            // Leather rather than iron: iron plate hides the whole body, leather
            // shows the rags and bone between the pieces.
            guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            guard.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            guard.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            guard.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            created.add(new StagePreview("Guard", "armed, in leather", "in leather", guard));
        }
        return List.copyOf(created);
    }

    private static RealPerson previewPerson(ClientLevel level, Kind kind, AgeStage stage, boolean asleep) {
        RealPerson person = Objects.requireNonNull(
                PersonEntityType.PERSON.get().create(level),
                "Could not create an age-lineup preview person");
        person.setAppearanceSeed(PREVIEW_SEED);
        person.setAppearanceGenes(AppearanceGenes.fromLegacySeed(PREVIEW_SEED));
        person.setKind(kind);
        person.setLifeStage(stage);
        if (asleep) {
            // Sleeping is a position, not a pose: the compositor reads it to
            // shut the eyes and the model reads it to rest the head, while the
            // standing pose keeps every body upright and comparable.
            person.setSleepingPos(BlockPos.ZERO);
        }
        return person;
    }

    /** Draws every stage from the same floor line and at the same camera scale. */
    private static void renderPerson(
            GuiGraphics graphics,
            int centerX,
            int baselineY,
            int entityScale,
            RealPerson person) {
        person.yBodyRot = 180.0F;
        person.setYRot(180.0F);
        person.setXRot(0.0F);
        person.yHeadRot = 180.0F;
        person.yHeadRotO = 180.0F;

        Quaternionf camera = new Quaternionf();
        Quaternionf pose = new Quaternionf().rotationZ((float) Math.PI).mul(camera);
        InventoryScreen.renderEntityInInventory(
                graphics,
                centerX,
                baselineY,
                entityScale,
                new Vector3f(),
                pose,
                camera,
                person);
    }

    private static String stageName(AgeStage stage) {
        String lower = stage.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private record StagePreview(String label, String detail, String shortDetail, RealPerson person) {
    }
}
