package ch.milosz.reactnative;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class RNZoomUsPackage extends TurboReactPackage {
    @Override
    @Nonnull
    public List<NativeModule> createNativeModules(@Nonnull ReactApplicationContext reactApplicationContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new RNZoomUsModule(reactApplicationContext));
        return modules;
    }

    @Nullable
    @Override
    public NativeModule getModule(@NonNull String name, @NonNull ReactApplicationContext reactApplicationContext) {
        if (name.equals(RNZoomUsModule.NAME)) {
            return new RNZoomUsModule(reactApplicationContext);
        } else {
            return null;
        }
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
            boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
            moduleInfos.put(
                    RNZoomUsModule.NAME,
                    new ReactModuleInfo(
                            RNZoomUsModule.NAME,
                            RNZoomUsModule.NAME,
                            false, // canOverrideExistingModule
                            false, // needsEagerInit
                            false, // isCxxModule
                            isTurboModule // isTurboModule
                    ));
            return moduleInfos;
        };
    }
}
