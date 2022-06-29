package com.example.myapplication1.ui.Home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.myapplication1.R
import com.example.myapplication1.databinding.FragmentHomeBinding
import com.example.myapplication1.ui.MapsActivity


class HomeFragment : Fragment() {

    var distanceUnit: Boolean = false
    var mapInterval: String = "1"
    var measureInterval: String = "1"
    var isCorrect: Boolean = false
    var isChecked:Boolean = false
    lateinit var binding: FragmentHomeBinding

    lateinit var b1: Button
    lateinit var txtFr1: TextView
    lateinit var txtUnit: TextView
    lateinit var edtxtInputDistance: EditText
    lateinit var edtxtInputTime: EditText
    lateinit var swMethod: Switch

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        val root: View = binding.root

        initializeUI()
        loadSettings()
        setUnit()


        edtxtInputTime.addTextChangedListener (object : android.text.TextWatcher{

            var len: Int = 0
            override fun afterTextChanged(p0: Editable?) {
                val str = edtxtInputTime.text.toString()
                if ((str.length == 2 || str.length == 5) && len < str.length) { //len check for backspace
                    edtxtInputTime.append("-")
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val str = edtxtInputTime.text.toString()
                len = str.length
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })




        b1.setOnClickListener {

            checkIfTimeIsCorrect(edtxtInputTime.getText().toString());
            checkIfDistanceIsNull(edtxtInputDistance.getText().toString());


            isChecked= swMethod.isChecked();

            if(isCorrect) {
                val intent = Intent(context, MapsActivity::class.java).apply {
                    putExtra("Distance", edtxtInputDistance.getText().toString());
                    putExtra("Time", edtxtInputTime.getText().toString());
                    putExtra("Method", isChecked);
                    putExtra("Unit", distanceUnit);
                    putExtra("MapInterval", Integer.parseInt(mapInterval));
                    putExtra("MeasureInterval", Integer.parseInt(measureInterval));
                }

                startActivity(intent)
            } else{
                Toast.makeText(context, R.string.error_incorrect_time,Toast.LENGTH_LONG).show()
            }
        }

        return root
    }

    fun initializeUI(){
        b1 = binding.btnFr1
        txtFr1 = binding.txtFr1
        txtUnit = binding.txtUnit
        edtxtInputDistance = binding.edtxtInputDistance
        edtxtInputTime = binding.edtxtInputTime
        swMethod = binding.swMethod
    }

    fun loadSettings() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            context
        )
        distanceUnit = sharedPreferences.getBoolean("units", false)
        mapInterval = sharedPreferences.getString("mapInterval", "1")!!
        measureInterval = sharedPreferences.getString("measureInterval", "1")!!
    }

    fun setUnit(){
        if(distanceUnit){

            txtUnit.setText("Feets")
        } else{
            txtUnit.setText("Meters")
        }
    }
    fun checkIfTimeIsCorrect(time: String) {
        val minute: Int
        val second: Int
        val min: String
        val sec: String
        min = time.substring(3, 4)
        sec = time.substring(6, 7)
        minute = min.toInt()
        second = sec.toInt()
        isCorrect = if (minute < 6 && second < 6 && time.length == 8) {
            true
        } else {
            false
        }
    }

    fun checkIfDistanceIsNull(input: String) {
        if (input[0] == '0') {
            isCorrect = false
        }
    }




}