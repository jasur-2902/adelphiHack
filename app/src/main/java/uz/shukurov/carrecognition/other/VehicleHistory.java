package uz.shukurov.carrecognition.other;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import uz.shukurov.carrecognition.R;

public class VehicleHistory extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_history);

        Intent intent = getIntent();

        String vehicle[] = intent.getStringArrayExtra("GET_EXTRA");
        String model = vehicle[1];
        String licencePlate = vehicle[0];

        AlertDialog.Builder builder = new AlertDialog.Builder(VehicleHistory.this);
        builder.setMessage("Sorry, we couldn't find any records for " + model + " and with Licence Plate Number - " + licencePlate)
                .setTitle("We couldn't find any records!");

        AlertDialog dialog = builder.create();

        dialog.show();

    }
}
