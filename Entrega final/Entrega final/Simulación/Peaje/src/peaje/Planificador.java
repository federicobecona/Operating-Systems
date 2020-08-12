package peaje;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Planificador implements Runnable{ 
    
    static int tiempo = 0;
    static boolean finalizador = false;
    
    @Override
    public void run() {
        while(true){
            try {
                Peaje.esperar.acquire(Peaje.NUMERO_CABINAS);  
                tiempo++;
                this.desbloquearVias(Cabina.tipoVehiculo.EMERGENCIA);
                this.desbloquearVias(Cabina.tipoVehiculo.NORMAL);
                this.avanzarVehiculosCabinas();
                this.asignarVehiculos();
                this.registrarDatos();
                for(Cabina cabina : Peaje.cabinas){
                    cabina.ejecutar();
                }
                if(finalizador){
                    break;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Planificador.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }
    }
    
    private void registrarDatos(){
        int cantidadCabinasVacias = 0;
        String datoPeaje = tiempo + ", ";                    
        String datoPeajeDibujo = "-----------------------------------------------------------------------------------------------------------------"+
                "----------------------------------"+
                "\n\n\n\n\n******************************************************************* TIEMPO: " + tiempo + " ********************************************************************\n";
        int nroCabinas = 1;
        for(Cabina cabina : Peaje.cabinas){            
            Vehiculo vehiculoActual = cabina.getVehiculoActual();
            datoPeajeDibujo += "\n---------------------------------------------------------------------------------------------------------"+
                    "------------------------------------------\n";
            datoPeajeDibujo += "EMERGENCIAS:                         |";
            int nroVehiculos =0;
            for(Vehiculo vehiculoEmergencia : cabina.getVehiculos(Cabina.tipoVehiculo.EMERGENCIA)){
                if(vehiculoEmergencia!=null){                    
                    if(vehiculoEmergencia.getEstaBloqueando()){                        
                        datoPeajeDibujo += vehiculoEmergencia.getMatricula()+" B";
                    }else{
                        datoPeajeDibujo += " "+vehiculoEmergencia.getMatricula()+" ";
                    }                                    
                }else{
                    datoPeajeDibujo += "   vacio  ";
                }
                if(nroVehiculos < cabina.getCapacidadVia()-1){
                    datoPeajeDibujo+=","; 
                }                
                nroVehiculos++;
            }
            datoPeajeDibujo += "\n              -> CASILLA " + nroCabinas + ": ";
            if(cabina.getVehiculoActual()!=null){
                 datoPeajeDibujo += cabina.getVehiculoActual().getMatricula()+" |";
            }else{
                datoPeajeDibujo += "  vacio  |";
            }
            datoPeajeDibujo+="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"+
                    "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
            datoPeajeDibujo += "NORMALES:                            |";
            nroVehiculos=0;
            for(Vehiculo vehiculoNormal : cabina.getVehiculos(Cabina.tipoVehiculo.NORMAL)){
                if(vehiculoNormal!=null){
                    if(vehiculoNormal.getEstaBloqueando()){                        
                        datoPeajeDibujo += vehiculoNormal.getMatricula()+" B";
                    }else{
                        datoPeajeDibujo += " "+vehiculoNormal.getMatricula()+" ";
                    } 
                }else{
                    datoPeajeDibujo += "   vacio  ";
                }                
                if(nroVehiculos < cabina.getCapacidadVia()-1){
                    datoPeajeDibujo+=","; 
                }
                nroVehiculos++;                
            }
            nroCabinas++;
            
            if(cabina.getVehiculoActual()!=null){                
                if(vehiculoActual.getTienePrioridad()){
                    datoPeaje += "EJECUTA " + vehiculoActual.getMatricula()+ " EMERGENCIA, " ;
                }
                else{
                    datoPeaje += "EJECUTA " + vehiculoActual.getMatricula()+ " NORMAL, ";
                }
            }else{
                if(cabina.getCantVehiculos(Cabina.tipoVehiculo.EMERGENCIA) > 0 &&
                        cabina.primerNormalOcupado()){
                    datoPeaje += "ESPERANDO POR " + cabina.getProximaEmergencia().getMatricula()+" EMERGENCIA, ";
                }else{
                    datoPeaje += "vacio, ";
                }                
            }
            if(cabina.estaVacia()){
                cantidadCabinasVacias++;
            }
        }
        Peaje.datosPeaje.add(datoPeaje);
        Peaje.datosPeajeDibujo.add(datoPeajeDibujo);
        finalizador = Peaje.vehiculos.isEmpty()&&
                cantidadCabinasVacias == Peaje.NUMERO_CABINAS;
    }
    
    private void asignarVehiculos(){
        int vehiculoCarril = 2;
        this.demorarVias(Cabina.tipoVehiculo.EMERGENCIA);
        this.demorarVias(Cabina.tipoVehiculo.NORMAL);
        while(!Peaje.vehiculos.isEmpty() && 
                Peaje.vehiculos.peek().getTiempoEntrada()<=Planificador.tiempo &&
                vehiculoCarril > 0){            
            Vehiculo vehiculo = Peaje.vehiculos.peek();
            Cabina cabinaAsignacionEmergencia=null;
            int cantidadEsperaEmergencias = Integer.MAX_VALUE;
            Cabina cabinaAsignacionNormal=null;
            int cantidadEsperaNormales = Integer.MAX_VALUE;
            for(Cabina cabina : Peaje.cabinas){
                if(!cabina.estaBloqueada(Cabina.tipoVehiculo.EMERGENCIA) &&
                        cabina.getCantVehiculos(Cabina.tipoVehiculo.EMERGENCIA) < cantidadEsperaEmergencias &&
                        !cabina.estaDemorada(Cabina.tipoVehiculo.EMERGENCIA)){                    
                    cantidadEsperaEmergencias = cabina.getCantVehiculos(Cabina.tipoVehiculo.EMERGENCIA);                    
                    cabinaAsignacionEmergencia = cabina;
                }
                if(!cabina.estaBloqueada(Cabina.tipoVehiculo.NORMAL) &&
                        cabina.getCantVehiculos(Cabina.tipoVehiculo.NORMAL) < cantidadEsperaNormales &&
                        !cabina.estaDemorada(Cabina.tipoVehiculo.NORMAL)){
                    cantidadEsperaNormales = cabina.getCantVehiculos(Cabina.tipoVehiculo.NORMAL);                    
                    cabinaAsignacionNormal = cabina;
                }                
            }            
            if(vehiculo.getTienePrioridad()){
                if(cabinaAsignacionEmergencia != null &&
                        cabinaAsignacionEmergencia.aceptaVehiculos(Cabina.tipoVehiculo.EMERGENCIA)){
                    cabinaAsignacionEmergencia.asignarVehiculo(Peaje.vehiculos.poll(),Cabina.tipoVehiculo.EMERGENCIA);
                }
            }
            else if(cabinaAsignacionNormal !=null &&
                    cabinaAsignacionNormal.aceptaVehiculos(Cabina.tipoVehiculo.NORMAL)){
                cabinaAsignacionNormal.asignarVehiculo(Peaje.vehiculos.poll(),Cabina.tipoVehiculo.NORMAL);
            }
            vehiculoCarril--;
        }
    }
    
    private void avanzarVehiculosCabinas(){
        for(Cabina cabina : Peaje.cabinas){
            cabina.tomarVehiculo();
            cabina.avanzarVehiculos(Cabina.tipoVehiculo.EMERGENCIA);
            cabina.avanzarVehiculos(Cabina.tipoVehiculo.NORMAL);
        }
    }    
    
    private void desbloquearVias(Cabina.tipoVehiculo tipoVehiculo) {
        for(int i=0; i<Peaje.NUMERO_CABINAS; i++){
            Cabina cabinaOrigen = Peaje.cabinas.get(i);
            Cabina cabinaDestino;            
            Vehiculo[] vehiculosEnCabina = cabinaOrigen.getVehiculos(tipoVehiculo);
            if(cabinaOrigen.estaBloqueada(tipoVehiculo) && Peaje.NUMERO_CABINAS>1){                                 
                if(i==0 || i == Peaje.NUMERO_CABINAS-1) {
                    int indice = i;
                    if(i==0){
                        indice++;
                    }else{
                        indice--;
                    }
                    cabinaDestino = Peaje.cabinas.get(indice);                       
                    for(int j=1;j < vehiculosEnCabina.length; j++){                        
                        if(vehiculosEnCabina[j-1]!= null && 
                                vehiculosEnCabina[j-1].getEstaBloqueando() &&
                                cabinaDestino.estaVaciaPosicionVehiculo(j-1,tipoVehiculo) &&
                                !cabinaDestino.estaBloqueada(tipoVehiculo)){
                            Vehiculo vehiculoActual = vehiculosEnCabina[j];                            
                            if(vehiculoActual != null){
                                //Estoy moviedo desde arriba 
                                if(i < indice){
                                    //Si el que muevo es de prioridad
                                    if(vehiculoActual.getTienePrioridad()){
                                        if(cabinaOrigen.getVehiculos(Cabina.tipoVehiculo.NORMAL)[j]==null){
                                            vehiculoActual.setFuiDistribuido(true);
                                            cabinaDestino.setPosicionVehiculo(j-1,vehiculoActual,tipoVehiculo);
                                            cabinaDestino.addCantVehiculosEnEspera(tipoVehiculo);
                                            cabinaOrigen.setPosicionVehiculo(j, null,tipoVehiculo);
                                            cabinaOrigen.decCantVehiculosEnEspera(tipoVehiculo);
                                        }
                                    }
                                    else{//El que muevo es normal
                                        if(cabinaDestino.getVehiculos(Cabina.tipoVehiculo.EMERGENCIA)[j]==null){
                                            vehiculoActual.setFuiDistribuido(true);
                                            cabinaDestino.setPosicionVehiculo(j-1,vehiculoActual,tipoVehiculo);
                                            cabinaDestino.addCantVehiculosEnEspera(tipoVehiculo);
                                            cabinaOrigen.setPosicionVehiculo(j, null,tipoVehiculo);
                                            cabinaOrigen.decCantVehiculosEnEspera(tipoVehiculo);
                                        }
                                    }                                    
                                }
                                else{//Me muevo de abajo para arriba
                                    //Si el que muevo es de prioridad
                                    if(vehiculoActual.getTienePrioridad()){
                                        if(cabinaDestino.getVehiculos(Cabina.tipoVehiculo.NORMAL)[j]==null){
                                            vehiculoActual.setFuiDistribuido(true);
                                            cabinaDestino.setPosicionVehiculo(j-1,vehiculoActual,tipoVehiculo);
                                            cabinaDestino.addCantVehiculosEnEspera(tipoVehiculo);
                                            cabinaOrigen.setPosicionVehiculo(j, null,tipoVehiculo);
                                            cabinaOrigen.decCantVehiculosEnEspera(tipoVehiculo);
                                        }
                                    }
                                    else{//El que muevo es normal
                                        if(cabinaOrigen.getVehiculos(Cabina.tipoVehiculo.EMERGENCIA)[j]==null){
                                            vehiculoActual.setFuiDistribuido(true);
                                            cabinaDestino.setPosicionVehiculo(j-1,vehiculoActual,tipoVehiculo);
                                            cabinaDestino.addCantVehiculosEnEspera(tipoVehiculo);
                                            cabinaOrigen.setPosicionVehiculo(j, null,tipoVehiculo);
                                            cabinaOrigen.decCantVehiculosEnEspera(tipoVehiculo);
                                        }
                                    } 
                                }
                            }
                        }
                    }
                }
                else if(Peaje.NUMERO_CABINAS > 2){                    
                    LinkedList <Cabina> cabinasDisponibles = new LinkedList<>();                    
                    Cabina cabinaIzquierda = Peaje.cabinas.get(i+1);
                    Cabina cabinaDerecha = Peaje.cabinas.get(i-1);                    
                    
                    for(int j=1;j < vehiculosEnCabina.length; j++){
                        cabinasDisponibles.clear();
                        Vehiculo vehiculoActual = vehiculosEnCabina[j];
                        if(vehiculoActual != null){                        
                            if(vehiculosEnCabina[j-1]!= null && 
                                vehiculosEnCabina[j-1].getEstaBloqueando() &&
                                cabinaIzquierda.estaVaciaPosicionVehiculo(j-1,tipoVehiculo) &&
                                    !cabinaIzquierda.estaBloqueada(tipoVehiculo)){                                    
                                if(vehiculoActual.getTienePrioridad()){
                                    if(cabinaOrigen.getVehiculos(Cabina.tipoVehiculo.NORMAL)[j]==null){
                                        cabinasDisponibles.add(cabinaIzquierda);
                                    }
                                }
                                else{
                                    if(cabinaIzquierda.getVehiculos(Cabina.tipoVehiculo.EMERGENCIA)[j]==null){
                                        cabinasDisponibles.add(cabinaIzquierda);
                                    }
                                }
                            }
                            if(vehiculosEnCabina[j-1]!= null && 
                                vehiculosEnCabina[j-1].getEstaBloqueando() &&
                                cabinaDerecha.estaVaciaPosicionVehiculo(j-1,tipoVehiculo)&&
                                    !cabinaDerecha.estaBloqueada(tipoVehiculo)){
                                if(vehiculoActual.getTienePrioridad()){
                                    if(cabinaDerecha.getVehiculos(Cabina.tipoVehiculo.NORMAL)[j]==null){
                                        cabinasDisponibles.add(cabinaDerecha);                                                        
                                    }
                                }
                                else{
                                    if(cabinaOrigen.getVehiculos(Cabina.tipoVehiculo.EMERGENCIA)[j]==null){
                                        cabinasDisponibles.add(cabinaDerecha);                                                        
                                    }
                                }                                
                            }                        

                            if(!cabinasDisponibles.isEmpty()){
                                Collections.shuffle(cabinasDisponibles);
                                cabinaDestino = cabinasDisponibles.getFirst();
                                vehiculoActual.setFuiDistribuido(true);
                                cabinaDestino.setPosicionVehiculo(j-1,vehiculoActual,tipoVehiculo);
                                cabinaDestino.addCantVehiculosEnEspera(tipoVehiculo);
                                cabinaOrigen.setPosicionVehiculo(j, null,tipoVehiculo);
                                cabinaOrigen.decCantVehiculosEnEspera(tipoVehiculo);
                            }                        
                        }
                    }
                }
            }
        }
    }
    
    
    private void demorarVias(Cabina.tipoVehiculo tipoVehiculo) {
        for(int i=0; i<Peaje.NUMERO_CABINAS; i++){
            Peaje.cabinas.get(i).setDemorarCabina(false,tipoVehiculo);
        }
        for(int i=0; i<Peaje.NUMERO_CABINAS; i++){
            Cabina cabinaOrigen = Peaje.cabinas.get(i);
            Cabina cabinaAdyacente; 
            
            if(cabinaOrigen.estaBloqueada(tipoVehiculo) && Peaje.NUMERO_CABINAS> 1 &&
                    cabinaOrigen.vehiculosTrancados(tipoVehiculo) > 0){
                if(i==0 || i == Peaje.NUMERO_CABINAS-1) {
                    int indice = i;
                    if(i==0){
                        indice++;
                    }else{
                        indice--;
                    }                    
                    cabinaAdyacente = Peaje.cabinas.get(indice);
                    if(!cabinaAdyacente.estaBloqueada(tipoVehiculo)){
                        cabinaAdyacente.setDemorarCabina(true,tipoVehiculo);
                    }
                }
                else if(Peaje.NUMERO_CABINAS > 2){                    
                    Cabina cabinaIzquierda = Peaje.cabinas.get(i-1);
                    Cabina cabinaDerecha = Peaje.cabinas.get(i+1);                    
                    if(!cabinaIzquierda.estaBloqueada(tipoVehiculo) && !cabinaDerecha.estaBloqueada(tipoVehiculo)){
                        if(cabinaIzquierda.getCantVehiculos(tipoVehiculo) < cabinaDerecha.getCantVehiculos(tipoVehiculo)){
                            cabinaAdyacente = cabinaIzquierda;
                        }
                        else{
                            cabinaAdyacente = cabinaDerecha;
                        }
                        cabinaAdyacente.setDemorarCabina(true,tipoVehiculo);
                    }
                    else{
                        if(!cabinaIzquierda.estaBloqueada(tipoVehiculo)){
                            cabinaAdyacente = cabinaIzquierda;
                            cabinaAdyacente.setDemorarCabina(true,tipoVehiculo);
                        }
                        else if(!cabinaDerecha.estaBloqueada(tipoVehiculo)){
                            cabinaAdyacente = cabinaDerecha;
                            cabinaAdyacente.setDemorarCabina(true,tipoVehiculo);
                        }
                    }
                }
            }
        }
    }    
}