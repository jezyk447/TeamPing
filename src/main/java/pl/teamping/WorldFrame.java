package pl.teamping;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public interface WorldFrame {
    Matrix4f projectionMatrix();
    Vec3d cameraPos();
    MatrixStack matrixStack();
    VertexConsumerProvider consumers();
}
