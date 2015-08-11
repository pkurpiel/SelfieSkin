package pl.waw.kurpiel.crio;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formUri = "https://collector.tracepot.com/083e6fec")
public class SelfieSkin extends Application {
    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        super.onCreate();
        ACRA.init(this);
    }
}