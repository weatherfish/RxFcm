package victoralbertos.io.rxfcm.presentation;

import android.app.Application;
import rx_fcm.internal.RxFcm;
import victoralbertos.io.rxfcm.AppFcmReceiverData;
import victoralbertos.io.rxfcm.AppFcmReceiverUIBackground;
import victoralbertos.io.rxfcm.RefreshTokenReceiver;


public class RxSampleApp extends Application {

    @Override public void onCreate() {
        super.onCreate();

        RxFcm.Notifications
                .init(this, new AppFcmReceiverData(), new AppFcmReceiverUIBackground());

        RxFcm.Notifications
            .onRefreshToken(new RefreshTokenReceiver());
    }
}
