package com.texelsaurus.minecraft.chameleon.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public abstract class ReplacementBlockPart implements ChameleonBlockModelPart {
    protected BlockModelPart parent;
    private TextureAtlasSprite sprite;
    private List<BakedQuad> quads = new ArrayList<>();

    public ReplacementBlockPart(BlockModelPart part, TextureAtlasSprite sprite) {
        parent = part;
        this.sprite = sprite;

        part.getQuads(null).forEach(quad -> quads.add(remapQuad(quad, sprite)));
        for (Direction dir : Direction.values()) {
            part.getQuads(dir).forEach(quad -> quads.add(remapQuad(quad, sprite)));
        }
    }

    public ReplacementBlockPart(BlockModelPart parent, BlockModelPart replacement) {
        this(parent, replacement.particleIcon());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return parent.useAmbientOcclusion();
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        if (sprite == null)
            return parent.particleIcon();

        return sprite;
    }

    public BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite sprite) {
        // Copy positions (avoid aliasing if the quad returns internal refs)
        Vector3fc[] pos = new Vector3fc[4];
        for (int i = 0; i < 4; i++) {
            Vector3fc p = quad.position(i);
            pos[i] = new Vector3f(p.x(), p.y(), p.z());
        }

        // Copy + remap packed UVs
        long[] uvs = new long[4];
        TextureAtlasSprite src = quad.sprite();

        for (int i = 0; i < 4; i++) {
            long packed = quad.packedUV(i);

            float u = unpackU(packed);
            float v = unpackV(packed);

            float unU = getUnInterpolatedU(src, u);
            float unV = getUnInterpolatedV(src, v);

            float newU = sprite.getU(unU);
            float newV = sprite.getV(unV);

            uvs[i] = packUV(newU, newV);
        }

        return new BakedQuad(
                pos[0],
                pos[1],
                pos[2],
                pos[3],
                uvs[0],
                uvs[1],
                uvs[2],
                uvs[3],
                quad.tintIndex(),
                quad.direction(),
                sprite,
                quad.shade(),
                quad.lightEmission()
        );
    }

    /**
     * Packed as: low 32 bits = u(float bits), high 32 bits = v(float bits).
     */
    private static long packUV(float u, float v) {
        return (Float.floatToRawIntBits(u) & 0xFFFFFFFFL)
                | ((long) Float.floatToRawIntBits(v) << 32);
    }

    private static float unpackU(long packedUV) {
        return Float.intBitsToFloat((int) (packedUV & 0xFFFFFFFFL));
    }

    private static float unpackV(long packedUV) {
        return Float.intBitsToFloat((int) (packedUV >>> 32));
    }

    private float getUnInterpolatedU(TextureAtlasSprite sprite, float u) {
        float diff = sprite.getU1() - sprite.getU0();
        return (u - sprite.getU0()) / diff;
    }

    private float getUnInterpolatedV(TextureAtlasSprite sprite, float v) {
        float diff = sprite.getV1() - sprite.getV0();
        return (v - sprite.getV0()) / diff;
    }
}
