-dontwarn com.petterp.floatingx.**
-keep public class com.petterp.floatingx.view.FxBasicContainerView{*;}
-keepclassmembers public class * extends com.petterp.floatingx.view.FxBasicContainerView {*;}
-keep public class com.petterp.floatingx.view.FxViewHolder{*;}
-keep public class com.petterp.floatingx.assist.FxScopeType{*;}
-keep public class com.petterp.floatingx.assist.FxDisplayMode{*;}
-keep public class com.petterp.floatingx.assist.FxAdsorbDirection{*;}
-keep public class com.petterp.floatingx.assist.FxGravity{*;}
-keep @kotlin.DslMarker class com.petterp.floatingx.assist.helper.FxBuilderDsl
-keep public class com.petterp.floatingx.util._FxScreenExt{
  private boolean checkNavigationBarShow(android.content.Context);
}

