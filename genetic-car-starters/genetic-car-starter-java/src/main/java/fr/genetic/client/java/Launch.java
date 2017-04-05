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
    int MAX_STEPS=5;

    protected void doMyAlgo() {

        for (int nbSteps = 0; nbSteps < MAX_STEPS ; nbSteps++) {
            System.out.println("Itération "+nbSteps);
            creation();
            evaluation();
            selection();
            croisement();
            mutation();
        }

        LOGGER.info("Mon champion est {}", carsSelected.get(0));
    }

    List<Car> carsCreated;
    List<CarView> carsViewCreated;
    List<CarScoreView> carsEvaluated;
    List<CarScoreView> carsSelected;
    List<CarScoreView> carsCroised;
    List<CarScoreView> carsMutated;

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
    }

    // Met à jour carsSelected;
    void selection() { // selection et croisement plusieurs itérations possibles
        CarScoreView champion = carsEvaluated.stream()
                .max((carScore1, carScore2) -> Float.compare(carScore1.score, carScore2.score))
                .get();
        carsSelected=new ArrayList<CarScoreView>();
        carsSelected.add(champion);

    };

    // Met à jour carCroised
    void croisement() {
        List<CarScoreView> premiereMoitie = carsEvaluated.subList(0, carsEvaluated.size()/2);
        List<CarScoreView> secondeMoitie = carsEvaluated.subList(carsEvaluated.size()/2,carsEvaluated.size());
        List<CarScoreView> croises = new ArrayList<CarScoreView>;
        for (int i = 0; i < carsEvaluated.size()/2;i++){
            premiereMoitie[0].
        }
        
        carsCroised=carsEvaluated;
                
    }

    // Met à jour carMutated
    void  mutation() {
        carsMutated=carsCroised;
    }


}
