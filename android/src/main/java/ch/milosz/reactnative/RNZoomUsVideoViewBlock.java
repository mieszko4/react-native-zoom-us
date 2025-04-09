package ch.milosz.reactnative;

import androidx.annotation.NonNull;

import android.util.Log;

import com.facebook.react.fabric.interop.UIBlockViewResolver;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;

import java.util.Iterator;
import java.util.List;

public class RNZoomUsVideoViewBlock implements UIBlock, com.facebook.react.fabric.interop.UIBlock {
    @Override
    public void execute(@NonNull NativeViewHierarchyManager nativeViewHierarchyManager) {
        executeImpl(nativeViewHierarchyManager, null);
    }

    @Override
    public void execute(@NonNull UIBlockViewResolver uiBlockViewResolver) {
        executeImpl(null, uiBlockViewResolver);
    }

    private void executeImpl(final NativeViewHierarchyManager nativeViewHierarchyManager, final UIBlockViewResolver uiBlockViewResolver) {
        synchronized (RNZoomUsModule.videoViews) {
            Iterator<Integer> iterator = RNZoomUsModule.videoViews.iterator();
            while (iterator.hasNext()) {
                final int tagId = iterator.next();
                final RNZoomUsVideoView view;
                try {
                    if (uiBlockViewResolver != null) {
                        view = (RNZoomUsVideoView) uiBlockViewResolver.resolveView(tagId);
                    } else {
                        view = (RNZoomUsVideoView) nativeViewHierarchyManager.resolveView(tagId);
                    }
                    if (view != null) view.update();
                } catch (Exception ex) {
                    Log.e(RNZoomUsModule.NAME, ex.getMessage());
                }
            }
        }
    }
}
