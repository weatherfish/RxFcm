/*
 * Copyright 2016 Victor Albertos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx_fcm.internal;

import android.app.Application;
import android.os.Bundle;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.observers.TestSubscriber;
import rx_fcm.Message;
import rx_fcm.TokenUpdate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class RxFcmTest {
    @Mock protected Application applicationMock;
    @Mock protected GetFcmServerToken getFcmServerTokenMock;
    @Mock protected ActivitiesLifecycleCallbacks activitiesLifecycle;
    @Mock protected GetFcmReceiversUIForeground getFcmForegroundReceivers;

    private final static String MOCK_TOKEN = "mock_token";

    @Before public void setUp()  {
        MockitoAnnotations.initMocks(this);
        RxFcm.Notifications.initForTesting(getFcmServerTokenMock, activitiesLifecycle,
            getFcmForegroundReceivers, new FcmReceiverDataMock(), new FcmReceiverMockUIBackground(),
            new FcmRefreshTokenReceiverMock());
        when(activitiesLifecycle.getApplication()).thenReturn(applicationMock);
    }

    @Test public void When_Call_Current_Token_Get_Token() {
        when(getFcmServerTokenMock.retrieve()).thenReturn(MOCK_TOKEN);

        TestSubscriber<String> subscriber = new TestSubscriber<>();
        RxFcm.Notifications.currentToken().subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        subscriber.assertValue(MOCK_TOKEN);
        subscriber.assertNoErrors();
    }

    @Test public void When_Call_On_Token_Refresh_Emit_Properly_Item() throws Exception {
        TestSubscriber<TokenUpdate> subscriber = FcmRefreshTokenReceiverMock.initSubscriber();

        when(getFcmServerTokenMock.retrieve()).thenReturn(MOCK_TOKEN);
        RxFcm.Notifications.onTokenRefreshed();
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
        TokenUpdate token1 = subscriber.getOnNextEvents().get(0);
        assertThat(token1.getToken(), is(MOCK_TOKEN));

        subscriber = FcmRefreshTokenReceiverMock.initSubscriber();
        reset(getFcmServerTokenMock);
        when(getFcmServerTokenMock.retrieve()).thenReturn(MOCK_TOKEN + 1);
        RxFcm.Notifications.onTokenRefreshed();
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
        TokenUpdate token2 = subscriber.getOnNextEvents().get(0);
        assertThat(token2.getToken(), is(MOCK_TOKEN + 1));

        reset(getFcmServerTokenMock);
        try {
            RxFcm.Notifications.onTokenRefreshed();
            subscriber.awaitTerminalEvent();
        } catch (Exception ignore) {
            assertThat(subscriber.getOnErrorEvents().size(), is(1));
            subscriber.assertValueCount(2);
        }
    }

    @Test public void When_Call_On_Fcm_Receiver_UI_Background_Notification_Emit_Properly_Item() {
        when(activitiesLifecycle.isAppOnBackground()).thenReturn(true);

        //FcmReceiver
        FcmReceiverDataMock.initSubscriber();

        //FcmReceiverUiBackground
        FcmReceiverMockUIBackground.initSubscriber();

        Bundle payload = new Bundle();
        String from1 = "MockServer1";
        RxFcm.Notifications.onNotificationReceived(from1, payload);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        String from2 = "MockServer2";
        RxFcm.Notifications.onNotificationReceived(from2, payload);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        //Check FcmReceiver
        List<Message> receiverMessages = FcmReceiverDataMock.getMessages();
        assertThat(receiverMessages.get(0).from(), is(from1));
        assertThat(receiverMessages.get(1).from(), is(from2));
        assertThat(receiverMessages.size(), is(2));

        //Check FcmReceiverBakgroundUI
        List<Message> receiverUIBackgroundMessages = FcmReceiverMockUIBackground.getMessages();
        assertThat(receiverUIBackgroundMessages.get(0).from(), is(from1));
        assertThat(receiverUIBackgroundMessages.get(1).from(), is(from2));
        assertThat(receiverUIBackgroundMessages.size(), is(2));

        //Check uireceiversbackground has been called only after receiver task has completed
        long onNotificationStartTimeStamp = FcmReceiverMockUIBackground.getOnNotificationStartTimeStamp();
        long onNotificationFinishTimeStamp = FcmReceiverDataMock.getOnNotificationFinishTimeStamp();

        assert onNotificationStartTimeStamp > onNotificationFinishTimeStamp;
    }


    @Test public void When_Call_On_Fcm_Receiver_UI_Foreground_Notification_Emit_Properly_Item() {
        when(activitiesLifecycle.isAppOnBackground()).thenReturn(false);

        //FcmReceiver
        FcmReceiverDataMock.initSubscriber();

        //FcmReceiverUI
        GetFcmReceiversUIForeground.Wrapper wrapperFcmReceiverUIForeground = new GetFcmReceiversUIForeground.Wrapper(new FcmReceiverMockUIForeground(), false);
        when(getFcmForegroundReceivers.retrieve(null, null)).thenReturn(wrapperFcmReceiverUIForeground);

        Bundle payload = new Bundle();
        String from1 = "MockServer1";
        RxFcm.Notifications.onNotificationReceived(from1, payload);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        String from2 = "MockServer2";
        RxFcm.Notifications.onNotificationReceived(from2, payload);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        //Check FcmReceiver
        List<Message> receiverMessages = FcmReceiverDataMock.getMessages();
        assertThat(receiverMessages.get(0).from(), is(from1));
        assertThat(receiverMessages.get(1).from(), is(from2));
        assertThat(receiverMessages.size(), is(2));

        //Check FcmReceiverForegroundUI
        FcmReceiverMockUIForeground fcmReceiverMockUIForeground = (FcmReceiverMockUIForeground) wrapperFcmReceiverUIForeground.fcmReceiverUIForeground();
        List<Message> messages = fcmReceiverMockUIForeground.getMessages();
        assertThat(messages.get(0).from(), is(from1));
        assertThat(messages.get(1).from(), is(from2));
        assertThat(messages.size(), is(2));

        //Check uireceiversforeground has been called only after receiver task has completed
        long onNotificationStartTimeStamp = fcmReceiverMockUIForeground.getOnNotificationStartTimeStamp();
        long onNotificationFinishTimeStamp = FcmReceiverDataMock.getOnNotificationFinishTimeStamp();

        assert onNotificationStartTimeStamp > onNotificationFinishTimeStamp;
    }
}