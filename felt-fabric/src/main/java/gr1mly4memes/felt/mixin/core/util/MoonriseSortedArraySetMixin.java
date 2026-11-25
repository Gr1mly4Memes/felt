package gr1mly4memes.felt.mixin.core.util;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.util.SortedArraySet;
import java.util.Arrays;
@Mixin(net.minecraft.util.SortedArraySet.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseSortedArraySetMixin<T> implements ca.spottedleaf.moonrise.patches.chunk_system.util.ChunkSystemSortedArraySet<T> {
    @Shadow @Final private java.util.Comparator<T> comparator;
    @Shadow private T[] contents;
    @Shadow private int size;
    @Shadow private int findIndex(T value) { throw new AssertionError(); }
    @Shadow private void addInternal(T value, int index) { throw new AssertionError(); }
    @Shadow private void removeInternal(int index) { throw new AssertionError(); }
    public final boolean removeIf(final java.util.function.Predicate<? super T> filter) {
        // prev. impl used an iterator, which could be n^2 and creates garbage
        int i = 0;
        final int len = this.size;
        final T[] backingArray = this.contents;

        for (;;) {
            if (i >= len) {
                return false;
            }
            if (!filter.test(backingArray[i++])) {
                continue;
            }
            break;
        }

        // we only want to write back to backingArray if we really need to

        int lastIndex = i - 1; // this is where new elements are shifted to

        for (; i < len; ++i) {
            final T curr = backingArray[i];
            if (!filter.test(curr)) { // if test throws we're screwed
                backingArray[lastIndex++] = curr;
            }
        }

        // cleanup end
        Arrays.fill(backingArray, lastIndex, len, null);
        this.size = lastIndex;
        return true;
    }

    @Override
    public final T moonrise$replace(final T object) {
        final int index = this.findIndex(object);
        if (index >= 0) {
            final T old = this.contents[index];
            this.contents[index] = object;
            return old;
        } else {
            this.addInternal(object, (-index - 1));
            return object;
        }
    }

    @Override
    public final T moonrise$removeAndGet(final T object) {
        int i = this.findIndex(object);
        if (i >= 0) {
            final T ret = this.contents[i];
            this.removeInternal(i);
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public final SortedArraySet<T> moonrise$copy() {
        final SortedArraySet<T> ret = SortedArraySet.create(this.comparator, 0);

        ((MoonriseSortedArraySetMixin<T>)(Object)ret).size = this.size;
        ((MoonriseSortedArraySetMixin<T>)(Object)ret).contents = Arrays.copyOf(this.contents, this.size);

        return ret;
    }

    @Override
    public Object[] moonrise$copyBackingArray() {
        return this.contents.clone();
    }

}
