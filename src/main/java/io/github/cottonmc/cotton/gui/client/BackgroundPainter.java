package io.github.cottonmc.cotton.gui.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import io.github.cottonmc.cotton.gui.widget.WItemSlot;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import juuxel.libninepatch.NinePatch;
import juuxel.libninepatch.TextureRegion;

import java.util.function.Consumer;

/**
 * Background painters are used to paint the background of a widget.
 * The background painter instance of a widget can be changed to customize the look of a widget.
 */
@FunctionalInterface
public interface BackgroundPainter {
	/**
	 * Paint the specified panel to the screen.
	 *
	 * @param context The draw context (GuiGraphics in 1.21.1)
	 * @param left    The absolute position of the left of the panel, in gui-screen coordinates
	 * @param top     The absolute position of the top of the panel, in gui-screen coordinates
	 * @param panel   The panel being painted
	 */
	public void paintBackground(GuiGraphics context, int left, int top, WWidget panel);

	/**
	 * The {@code VANILLA} background painter draws a vanilla-like GUI panel using nine-patch textures.
	 *
	 * <p>This background painter uses {@code libgui:textures/widget/panel_light.png} as the light texture and
	 * {@code libgui:textures/widget/panel_dark.png} as the dark texture.
	 */
	public static BackgroundPainter VANILLA = createLightDarkVariants(
		createNinePatch(ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/panel_light.png")),
		createNinePatch(ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/panel_dark.png"))
	);

	/**
	 * The {@code SLOT} background painter draws item slots or slot-like widgets.
	 */
	public static BackgroundPainter SLOT = (context, left, top, panel) -> {
		if (!(panel instanceof WItemSlot)) {
			ScreenDrawing.drawBeveledPanel(context, left-1, top-1, panel.getWidth()+2, panel.getHeight()+2, 0xB8000000, 0x4C000000, 0xB8FFFFFF);
		} else {
			WItemSlot slot = (WItemSlot)panel;
			for(int x = 0; x < slot.getWidth()/18; ++x) {
				for(int y = 0; y < slot.getHeight()/18; ++y) {
					int index = x + y * (slot.getWidth() / 18);
					float px = 1 / 64f;
					if (slot.isBigSlot()) {
						int sx = (x * 18) + left - 4;
						int sy = (y * 18) + top - 4;
						ScreenDrawing.texturedRect(context, sx, sy, 26, 26, WItemSlot.SLOT_TEXTURE,
							18 * px, 0, 44 * px, 26 * px, 0xFF_FFFFFF);
						if (slot.getFocusedSlot() == index) {
							ScreenDrawing.texturedRect(context, sx, sy, 26, 26, WItemSlot.SLOT_TEXTURE,
								18 * px, 26 * px, 44 * px, 52 * px, 0xFF_FFFFFF);
						}
					} else {
						int sx = (x * 18) + left;
						int sy = (y * 18) + top;
						ScreenDrawing.texturedRect(context, sx, sy, 18, 18, WItemSlot.SLOT_TEXTURE,
							0, 0, 18 * px, 18 * px, 0xFF_FFFFFF);
						if (slot.getFocusedSlot() == index) {
							ScreenDrawing.texturedRect(context, sx, sy, 18, 18, WItemSlot.SLOT_TEXTURE,
								0, 26 * px, 18 * px, 44 * px, 0xFF_FFFFFF);
						}
					}
				}
			}
		}
	};

	public static BackgroundPainter createColorful(int panelColor) {
		return (context, left, top, panel) -> {
			ScreenDrawing.drawGuiPanel(context, left, top, panel.getWidth(), panel.getHeight(), panelColor);
		};
	}

	public static BackgroundPainter createColorful(int panelColor, float contrast) {
		return (context, left, top, panel) -> {
			int shadowColor = ScreenDrawing.multiplyColor(panelColor, 1.0f - contrast);
			int hilightColor = ScreenDrawing.multiplyColor(panelColor, 1.0f + contrast);

			ScreenDrawing.drawGuiPanel(context, left, top, panel.getWidth(), panel.getHeight(), shadowColor, panelColor, hilightColor, 0xFF000000);
		};
	}

	public static NinePatchBackgroundPainter createNinePatch(ResourceLocation texture) {
		return createNinePatch(new Texture(texture), builder -> builder.cornerSize(4).cornerUv(0.25f));
	}

	public static NinePatchBackgroundPainter createNinePatch(Texture texture, Consumer<NinePatch.Builder<ResourceLocation>> configurator) {
		TextureRegion<ResourceLocation> region = new TextureRegion<>(texture.image(), texture.u1(), texture.v1(), texture.u2(), texture.v2());
		var builder = NinePatch.builder(region);
		configurator.accept(builder);
		return new NinePatchBackgroundPainter(builder.build());
	}

	public static BackgroundPainter createLightDarkVariants(BackgroundPainter light, BackgroundPainter dark) {
		return (context, left, top, panel) -> {
			if (panel.shouldRenderInDarkMode()) dark.paintBackground(context, left, top, panel);
			else light.paintBackground(context, left, top, panel);
		};
	}
}
