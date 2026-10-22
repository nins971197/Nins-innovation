package com.truenins.client.domain.judgment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bakes one block's emitted vertices per block state and replays them afterwards.
 *
 * <p>The judgment stage draws roughly 1.4k blocks per frame. Each
 * {@code BlockRenderDispatcher.renderSingleBlock} call re-resolves the model, walks its quads and
 * recomputes per-face shade and tint for geometry that never changes; only the transform does. The
 * bake runs the exact same renderer once per distinct state into a recording consumer, so the
 * replayed vertex stream is byte-identical to what the block renderer produced, while the per-block
 * cost drops to a matrix multiply and a handful of buffer writes.
 */
public final class BlockMeshCache {
    /** Matches PixelGeometry.FULL_BRIGHT: light 15/15, the stage is never lit by the world. */
    private static final int PACKED_LIGHT = 0xF000F0;

    private static final Map<BlockState, Template> CACHE = new HashMap<>();

    private BlockMeshCache() {}

    /** Emits one block with the cached template, honouring the same translate-then-scale order. */
    public static void emit(PoseStack pose, MultiBufferSource buffers, BlockState state,
                            double x, double y, double z, double sx, double sy, double sz) {
        Template template = CACHE.get(state);
        if (template == null) {
            template = bake(state);
            CACHE.put(state, template);
        }
        if (template.ops.isEmpty()) return;
        VertexConsumer out = buffers.getBuffer(template.type);
        Matrix4f model = new Matrix4f(pose.last().pose());
        model.translate((float) x, (float) y, (float) z);
        model.scale((float) sx, (float) sy, (float) sz);
        Matrix3f normal = new Matrix3f(pose.last().normal());
        List<Op> ops = template.ops;
        for (int i = 0; i < ops.size(); i++) {
            Op op = ops.get(i);
            switch (op.kind) {
                case POSITION -> out.vertex(model, op.a, op.b, op.c);
                case COLOR_INT -> out.color(op.i0, op.i1, (int) op.c, (int) op.d);
                case COLOR_FLOAT -> out.color(op.a, op.b, op.c, op.d);
                case UV -> out.uv(op.a, op.b);
                case OVERLAY_UV -> out.overlayCoords((int) op.a, (int) op.b);
                case OVERLAY_PACKED -> out.overlayCoords(op.i0);
                case UV2_UV -> out.uv2((int) op.a, (int) op.b);
                case UV2_PACKED -> out.uv2(op.i0);
                case NORMAL -> out.normal(normal, op.a, op.b, op.c);
                case END -> out.endVertex();
            }
        }
    }

    private static Template bake(BlockState state) {
        Recorder recorder = new Recorder();
        PoseStack pose = new PoseStack();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, recorder,
            PACKED_LIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        RenderType type = recorder.type == null ? RenderType.solid() : recorder.type;
        return new Template(type, recorder.ops);
    }

    private static final class Template {
        final RenderType type;
        final List<Op> ops;

        Template(RenderType type, List<Op> ops) {
            this.type = type;
            this.ops = ops;
        }
    }

    private record Op(int kind, float a, float b, float c, float d, int i0, int i1) {}

    private static final int POSITION = 1, COLOR_INT = 2, COLOR_FLOAT = 3, UV = 4, OVERLAY_UV = 5,
        OVERLAY_PACKED = 6, UV2_UV = 7, UV2_PACKED = 8, NORMAL = 9, END = 10;

    /** Captures the renderer's vertex stream once; the transform at bake time is the identity. */
    private static final class Recorder implements MultiBufferSource, VertexConsumer {
        RenderType type;
        final List<Op> ops = new ArrayList<>();

        @Override public VertexConsumer getBuffer(RenderType type) {
            if (this.type == null) this.type = type;
            return this;
        }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            ops.add(new Op(POSITION, (float) x, (float) y, (float) z, 0f, 0, 0));
            return this;
        }

        @Override public VertexConsumer color(int r, int g, int b, int a) {
            ops.add(new Op(COLOR_INT, 0f, 0f, b, a, r, g));
            return this;
        }

        @Override public VertexConsumer color(float r, float g, float b, float a) {
            ops.add(new Op(COLOR_FLOAT, r, g, b, a, 0, 0));
            return this;
        }

        @Override public VertexConsumer uv(float u, float v) {
            ops.add(new Op(UV, u, v, 0f, 0f, 0, 0));
            return this;
        }

        @Override public VertexConsumer overlayCoords(int u, int v) {
            ops.add(new Op(OVERLAY_UV, u, v, 0f, 0f, 0, 0));
            return this;
        }

        @Override public VertexConsumer overlayCoords(int packed) {
            ops.add(new Op(OVERLAY_PACKED, 0f, 0f, 0f, 0f, packed, 0));
            return this;
        }

        @Override public VertexConsumer uv2(int u, int v) {
            ops.add(new Op(UV2_UV, u, v, 0f, 0f, 0, 0));
            return this;
        }

        @Override public VertexConsumer uv2(int packed) {
            ops.add(new Op(UV2_PACKED, 0f, 0f, 0f, 0f, packed, 0));
            return this;
        }

        @Override public VertexConsumer normal(float x, float y, float z) {
            ops.add(new Op(NORMAL, x, y, z, 0f, 0, 0));
            return this;
        }

        @Override public void endVertex() {
            ops.add(new Op(END, 0f, 0f, 0f, 0f, 0, 0));
        }

        @Override public void defaultColor(int r, int g, int b, int a) {}

        @Override public void unsetDefaultColor() {}
    }
}
