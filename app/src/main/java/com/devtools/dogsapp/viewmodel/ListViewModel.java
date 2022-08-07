package com.devtools.dogsapp.viewmodel;

import android.app.Application;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.devtools.dogsapp.model.DogBreed;
import com.devtools.dogsapp.model.DogDao;
import com.devtools.dogsapp.model.DogDatabase;
import com.devtools.dogsapp.model.DogsApiService;
import com.devtools.dogsapp.util.NotificationsHelper;
import com.devtools.dogsapp.util.SharedPreferencesHelper;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class ListViewModel extends AndroidViewModel {

    public MutableLiveData<List<DogBreed>> dogs = new MutableLiveData<>();
    public MutableLiveData<Boolean> dogsLoadError = new MutableLiveData<>();
    public MutableLiveData<Boolean> loading = new MutableLiveData<>();

    private AsyncTask<List<DogBreed>, Void, List<DogBreed>> insertTask;
    private AsyncTask<Void, Void, List<DogBreed>> retrieveTask;

    private DogsApiService dogsService = new DogsApiService();
    private CompositeDisposable disposable = new CompositeDisposable();

    private SharedPreferencesHelper prefHelper = SharedPreferencesHelper.getInstance(getApplication());
    private long refreshTime = 5 * 60 * 1000 * 1000 * 1000L; // nano-Seconds

    public ListViewModel(@NonNull Application application) {
        super(application);
    }

    public void refresh() {
        long updateTime = prefHelper.getUpdateTime();
        long currentTime = System.nanoTime();
        checkCacheDuration();

        if(updateTime != 0 && currentTime - updateTime < refreshTime) {
            fetchFromDatabase();
        } else {
            fetchFromRemote();
        }

//        fetchFromRemote();
        fetchFromDatabase();
    }

    private void checkCacheDuration() {
        String cachePreference = prefHelper.getCacheDuration();
        if(!cachePreference.equals("")) {
            try {
                int cachePreferenceInt = Integer.parseInt(cachePreference);
                refreshTime = cachePreferenceInt * 1000 * 1000 * 1000L;
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

    }

    public void refreshBypassCache() {
        fetchFromRemote();

    }
    private void fetchFromDatabase() {
        loading.setValue(true);
        retrieveTask = new RetrieveDogsTask();
        retrieveTask.execute();
    }

    private void fetchFromRemote() {
        // set initial values prior to call
        loading.setValue(true);
        dogsLoadError.setValue(false);
        disposable.add(
            dogsService.getDogs()
                // do operation on new tread
                .subscribeOn(Schedulers.newThread())
                // view on main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<DogBreed>>() {
                    @Override
                    public void onSuccess(List<DogBreed> dogBreeds) {
                        insertTask = new InsertDogsTask();
                        insertTask.execute(dogBreeds);
                        dogsRetrieved( dogBreeds);
                        Toast.makeText(getApplication(),"Dogs received from EndPoint", Toast.LENGTH_SHORT).show();
                        NotificationsHelper.getInstance(getApplication()).createNotification();
                    }

                    @Override
                    public void onError(Throwable e) {
                        dogsLoadError.setValue(true);
                        loading.setValue(false);
                        e.printStackTrace();
                    }
                })
        );

    }



    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear();
        if(insertTask != null) {
            insertTask.cancel(true);
            insertTask = null;
        }
        if(retrieveTask != null) {
            retrieveTask.cancel(true);
            retrieveTask = null;
        }

    }

    private void dogsRetrieved(List<DogBreed> dogBreeds) {
        dogs.setValue(dogBreeds);
        dogsLoadError.setValue(false);
        loading.setValue(false);
    }

    private class InsertDogsTask extends AsyncTask<List<DogBreed>, Void, List<DogBreed>> {

        @Override
        protected List<DogBreed> doInBackground(List<DogBreed>... lists) {
            List<DogBreed> list = lists[0];
            DogDao dao = DogDatabase.getInstance(getApplication()). dogDao();
            dao.deleteAllDogs();
            ArrayList<DogBreed> newList =  new ArrayList<>(list);
            List<Long> result = dao.insertAll(newList.toArray(new DogBreed[0]));

            // adding uuid to each
//            int i = 0;
//            while (i < list.size()) {
//                list.get(i).uuid = result.get(i);
//                ++;
//            }

            for(int i=0; i<list.size(); i++) {
                list.get(i).uuid = result.get(i).intValue();
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<DogBreed> dogBreeds) {
            dogsRetrieved(dogBreeds);
            prefHelper.saveUpdateTime(System.nanoTime());
        }
    }

    private class RetrieveDogsTask extends AsyncTask<Void, Void, List<DogBreed>> {

        @Override
        protected List<DogBreed> doInBackground(Void... voids) {
            return DogDatabase.getInstance(getApplication()).dogDao().getAllDogs();
        }

        @Override
        protected void onPostExecute(List<DogBreed> dogBreeds) {
            dogsRetrieved(dogBreeds);
            Toast.makeText(getApplication(),"Dogs received from Database", Toast.LENGTH_SHORT).show();
        }
    }
}
