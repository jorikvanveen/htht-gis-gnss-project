package com.example.gnss.singleton;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

import com.esotericsoftware.kryo.kryo5.serializers.DefaultSerializers;
import com.example.gnss.dto.Answer;
import com.example.gnss.dto.BooleanAnswer;
import com.example.gnss.dto.FloatAnswer;
import com.example.gnss.dto.IntAnswer;
import com.example.gnss.dto.StringAnswer;
import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyDataPoint;
import com.example.gnss.dto.SurveyQuestion;
import com.example.gnss.dto.SurveyQuestionType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataVault implements Serializable {
    private static volatile @Nullable DataVault instance = null;
    private static volatile boolean isCreating = false;
    private static volatile boolean didLoad = false;
    private static final Kryo kryo = new Kryo();
    private static final Lock loading = new ReentrantLock();

    private @NonNull HashMap<UUID, Survey> surveys;
    private @NonNull HashMap<UUID, ArrayList<SurveyDataPoint>> entries;


    private DataVault() {
        this.surveys = new HashMap<UUID, Survey>();
        this.entries = new HashMap<>();
    }

    public Optional<Survey> getSurvey(UUID id) {
        Survey survey = this.surveys.get(id);
        if (survey == null) {
            return Optional.empty();
        }

        return Optional.of(survey);
    }

    public Collection<Survey> surveys() {
        return this.surveys.values();
    }



    public ArrayList<SurveyDataPoint> getSurveyEntries(UUID survey_id) {
        ArrayList<SurveyDataPoint> entries = this.entries.get(survey_id);
        if (entries == null) {
            Log.w("GNSS", "WARNING: Survey was not found. ID: " + survey_id.toString());
            return new ArrayList<>();
        }

        return entries;
    }


    public void addSurvey(Survey survey) {
        this.surveys.put(survey.getId(), survey);
    }

    public void saveEntry(UUID survey_id, SurveyDataPoint entry) {
        ArrayList<SurveyDataPoint> entries = this.entries.computeIfAbsent(survey_id, k -> new ArrayList<>());
        entries.add(entry);
    }

    public static void save(@NonNull Context context) throws IOException {
        Output out;
        try {
            out = new Output(context.openFileOutput("data.bin", Context.MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to open output file");
        }

        if (instance == null) {
            return;
        }

        kryo.writeObject(out, instance);
        out.flush();
        out.close();
    }

    public static DataVault getInstance(Context context) {
        if (instance != null) {
            return instance;
        }

        if (isCreating) {
            try {
                loading.wait();
                assert instance != null;
                return instance;
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for DataVault creation", e);
            }
        }

        isCreating = true;

        DataVault vault = null;
        synchronized (loading) {
            try {
                vault = DataVault.loadOrCreate(context);
                instance = vault;
                loading.notifyAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        isCreating = false;
        return vault;
    }

    @SuppressLint("NewApi")
    private static DataVault loadOrCreate(Context context) throws IOException {
        if (didLoad) {
            throw new RuntimeException("DataVault.loadOrCreate called twice! Something is very wrong!");
        }

        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
        kryo.register(UUID.class, new DefaultSerializers.UUIDSerializer());

        kryo.register(Survey.class);
        kryo.register(SurveyQuestion.class);
        kryo.register(SurveyQuestionType.class);
        kryo.register(DataVault.class);
        kryo.register(Answer.class);
        kryo.register(IntAnswer.class);
        kryo.register(BooleanAnswer.class);
        kryo.register(StringAnswer.class);
        kryo.register(FloatAnswer.class);
        kryo.register(SurveyDataPoint.class);

        Input input;
        try {
            input = new Input(context.openFileInput("data.bin"));
        } catch (FileNotFoundException e) {
            Log.d("GNSS", "No existing vault found, creating new");
            return new DataVault();
        }

        DataVault vault = kryo.readObject(new Input(input), DataVault.class);
        input.close();

        didLoad = true;
        return vault;
    }

    public void readFile(byte[] inputFile) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputFile);
        Input input = new Input(inputStream);

        Survey inputSurvey = kryo.readObject(input, Survey.class);
        input.close();

        surveys.put(inputSurvey.getId(), inputSurvey);
    }
}
