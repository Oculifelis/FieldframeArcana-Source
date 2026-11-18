package dev.fieldframe.arcana.mixin.client;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Test clients only (-Dfieldframe_arcana.autotest set): the game window opens off-screen without taking focus or
 * jumping to the front, so a muted test run never interrupts whoever is at the PC. Normal play is untouched.
 */
@Mixin(Window.class)
public class WindowNoFocusMixin {
    @Inject(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private static void fieldframe_arcana$noFocus(CallbackInfoReturnable<Long> cir) {
        if (System.getProperty("fieldframe_arcana.autotest") != null) {
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_POSITION_X, -32000);
            GLFW.glfwWindowHint(GLFW.GLFW_POSITION_Y, -32000);
        }
    }
}
