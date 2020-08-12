package peaje;

import java.io.BufferedReader; 
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class Peaje {   
    
    static final int NUMERO_CABINAS = 3;
    static final int DEMORA_CABINAS = 3;
    static LinkedList<String> datosPeaje = new LinkedList<>();
    static LinkedList<String> datosPeajeDibujo = new LinkedList<>();
    
    static Thread reloj = new Thread(new Planificador());
    static ArrayList<Cabina> cabinas = new ArrayList<>();
    static Queue<Vehiculo> vehiculos = new LinkedList<>();

    static Semaphore esperar = new Semaphore(0);
 
    public static void main(String[] args) throws IOException, InterruptedException {
        Peaje.leerVehiculos(); 
        String datoPeaje = "Tiempo, ";  
        for(int i = 1 ; i <= NUMERO_CABINAS; i++){
            datoPeaje += "Cabina" + i + ", ";
            Semaphore ejecutar = new Semaphore(1);
            Cabina cabina = new Cabina(ejecutar);
            cabinas.add(cabina);
        }
        datosPeaje.add(datoPeaje);
        reloj.start();
        for(Cabina cabina : cabinas){
            cabina.start();
        }
        reloj.join();
        for(Cabina cabina : cabinas){
            cabina.join();
        }
        Peaje.escribirVehiculos("salidaPeajeEnCabina.csv",datosPeaje);
        Peaje.escribirVehiculos("salidaPeajeDibujo.txt",datosPeajeDibujo);
    }   
    
    public static void leerVehiculos() throws FileNotFoundException, IOException{
        try (FileReader lector = new FileReader("entradaPeajePrueba.csv"); 
                BufferedReader brLector = new BufferedReader(lector)) {
            String nuevaLinea = brLector.readLine();
            while(nuevaLinea!=null){
                String[] infoVehiculo = nuevaLinea.split(",");
                Vehiculo vehiculoAux = new Vehiculo(infoVehiculo[0], 
                        Integer.parseInt(infoVehiculo[1]),
                        Integer.parseInt(infoVehiculo[2]),
                        Integer.parseInt(infoVehiculo[3]),
                        Integer.parseInt(infoVehiculo[4])
                );
                vehiculos.add(vehiculoAux);
                nuevaLinea = brLector.readLine();
            }
            brLector.close();
            lector.close();
        }       
    }
    
    public static void escribirVehiculos(String nombreArchivo, LinkedList<String> datos) throws IOException, InterruptedException{
        try (FileWriter escritor = new FileWriter(nombreArchivo, false);
            BufferedWriter bwEscritor = new BufferedWriter(escritor)) {
            for (int i = 0; i < datos.size(); i++) {
                String lineaActual = datos.get(i);
                bwEscritor.write(lineaActual);
                bwEscritor.newLine();
            }
            bwEscritor.close();
            escritor.close();
        }
    }
    
}