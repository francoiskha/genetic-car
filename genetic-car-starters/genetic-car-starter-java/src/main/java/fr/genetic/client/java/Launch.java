package fr.genetic.client.java;

import fr.genetic.client.java.algo.Car;
import fr.genetic.client.java.api.CarScoreView;
import fr.genetic.client.java.api.CarView;
import fr.genetic.client.java.api.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
public class Launch implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Launch.class);

    @Value("${genetic.server.host}")
    private String host;

    @Autowired
    private RestTemplate restTemplate;

    private Team team = Team.RED;


    public static void main(String[] args) {
        SpringApplication.run(Launch.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            doMyAlgo();
        } catch (RestClientException restException) {
            LOGGER.error(restException.getMessage());
        }
    }
    int MAX_STEPS=50;

    protected void doMyAlgo() {
        creation();
        for (int nbSteps = 0; nbSteps < MAX_STEPS ; nbSteps++) {
            System.out.println("Itération " + nbSteps);
            evaluation();
            System.out.println("- " + carsEvaluated.size() + " evaluated");
            selection();
            System.out.println("- " + carsSelected.size() + " selected");
            //croisement();
            croisementBis();
            System.out.println("- " + carsCroised.size() + " croised");
            mutation();
            System.out.println("- " + carsMutated.size() + " mutated");

            report(nbSteps);
            prepareIterationSuivante();
            System.out.println("- " + carsCreated.size() + " préparés pour l'étape suivante");

        }
        evaluation();
        LOGGER.info("Mon champion est {}", carsSelected.get(0));
    }

    List<Car> carsCreated;
    List<CarView> carsViewCreated;
    List<CarScoreView> carsEvaluated; // 20
    List<CarScoreView> carsSelected; // 8
    List<CarScoreView> carsCroised; // 4 ( 1 enfant par paire)
    List<CarScoreView> carsMutated; // 4

    // Met à jour carsCreated;
    void creation() {
        carsCreated=new ArrayList<Car>();
        carsViewCreated=new ArrayList<CarView>();

        for (int i=0;i<20;i++) {
            Car c= Car.random();
            carsCreated.add(c);
            carsViewCreated.add(c.toCarView());
        }
    }

    // Met à jour carsEvaluated;
    void evaluation() {
        String url = host + "/simulation/evaluate/" + team.name();
        carsEvaluated= restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity(carsViewCreated), new ParameterizedTypeReference<List<CarScoreView>>() {}).getBody();

        carsEvaluated.stream().forEach(carScoreView -> System.out.println(carScoreView.toString()));
    }

    // Met à jour carsSelected;
    void selection() { // selection et croisement plusieurs itérations possibles

        int targetScore = 50;

        System.out.println("Sorted cars" + carsEvaluated.size());


        List<CarScoreView> sortedCars =  carsEvaluated.stream()
                .sorted((carScore1, carScore2) -> -1 * Float.compare(carScore1.score, carScore2.score))
                .collect(Collectors.toList());

        carsSelected = sortedCars.stream().filter(carScoreView -> carScoreView.score > targetScore).collect(Collectors.toList());

    };

    // Met à jour carCroised
    void croisement() {
        List<CarScoreView> premiereMoitie = carsEvaluated.subList(0, carsEvaluated.size()/2);
        List<CarScoreView> secondeMoitie = carsEvaluated.subList(carsEvaluated.size()/2,carsEvaluated.size());
        List<CarScoreView> croises = new ArrayList<CarScoreView>();
        for (int i = 0; i < carsEvaluated.size()/2;i++){
            int numeroDeGene = Double.valueOf(Math.floor(fr.genetic.client.java.algo.Random.next(0,22))).intValue();
            Car croise = Car.createFrom(premiereMoitie.get(i).car);
            croise.coords[numeroDeGene] = Car.createFrom(secondeMoitie.get(i).car).coords[numeroDeGene];
            CarScoreView result = new CarScoreView();
            result.car = croise.toCarView();
            croises.add(result);
        }
        carsCroised=croises;
    }

    void croisementBis() {
        if (carsSelected.size() > 2) {
            List<CarScoreView> premiereMoitie = carsSelected.subList(0, carsSelected.size() / 2);
            List<CarScoreView> secondeMoitie = carsSelected.subList(carsSelected.size() / 2, carsSelected.size());

            List<CarScoreView> croises = new ArrayList<CarScoreView>();

            for (int i = 0; i < carsEvaluated.size() / 2; i++) {

                double selRange = Math.floor(fr.genetic.client.java.algo.Random.next(0, 100));

                CarView croise = premiereMoitie.get(i).car;
                CarView autreParent = secondeMoitie.get(i).car;

                if (selRange < 33) {
                    //Roue 1
                    croise.wheel1 = autreParent.wheel1;

                } else if (selRange < 66) {
                    //Roue 2
                    croise.wheel2 = autreParent.wheel2;

                } else {
                    // Roue 3
                    croise.chassis = autreParent.chassis;

                }

                CarScoreView result = new CarScoreView();
                result.car = croise;
                croises.add(result);
            }

            carsCroised = croises;
        } else {
            carsCroised = new ArrayList<>();
        }

    }

    // Met à jour carMutated
    void  mutation() {

        Random ran = new Random(System.currentTimeMillis());
        carsCroised.stream().map(carScoreView -> mutateCarScoreView(ran.nextInt(100), carScoreView)).collect(Collectors.toList());

        carsMutated=carsCroised;
    }

    private CarScoreView mutateCarScoreView(int probaMutation, CarScoreView carScoreView) {
        double txMutation = 25;

        if (probaMutation < txMutation ) {

            Car mutationElement = Car.random();
            Random ran = new Random(System.currentTimeMillis());
            int target = ran.nextInt(22);

            Car rawMutatedCar = Car.createFrom(carScoreView.car);

            rawMutatedCar.coords[target] = mutationElement.coords[target];
            carScoreView.car = rawMutatedCar.toCarView();
        }

        return carScoreView;

    }


    // Prépare le nouveau carsCreated pour l'itération siuvante
    void prepareIterationSuivante() {
        carsCreated=new ArrayList<Car>();
        carsCreated.addAll(carsSelected.stream().map(carScoreView -> Car.createFrom(carScoreView.car)).collect(Collectors.toList())); // 8
        System.out.println("Cars Created  suivante : " + carsCreated.size());
        carsCreated.addAll(carsCroised.stream().map(carScoreView -> Car.createFrom(carScoreView.car)).collect(Collectors.toList()));  // 4
        System.out.println("Cars Created croised suivante : " + carsCreated.size());
        carsCreated.addAll(carsMutated.stream().map(carScoreView -> Car.createFrom(carScoreView.car)).collect(Collectors.toList())); // 8
        System.out.println("Cars Created mutated suivante : " + carsCreated.size());

        // Génération de 4 randoms
        while(carsCreated.size() < 20) {
            Car c = Car.random();
            carsCreated.add(c);
        }

        if (carsCreated.size() > 0) {
            carsCreated = carsCreated.subList(0, 20);
        }

        System.out.println("Cars Created iteratino suivante : " + carsCreated.size());

        carsViewCreated.clear();
        carsViewCreated = carsCreated.stream().map(Car::toCarView).collect(Collectors.toList());
    }

    void report(int i) {
        System.out.println("itération " + i);
        for (CarScoreView csv : carsSelected) {
            System.out.print(csv.score+",");
        }
        System.out.println();

    }

}
