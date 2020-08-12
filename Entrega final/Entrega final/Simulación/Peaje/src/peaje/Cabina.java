package peaje;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cabina extends Thread{    
    
    private final int capacidadVia = 10;
    private int cantNormalesEspera = 0;
    private int cantEmergenciasEspera = 0;
    private boolean demoraPorRedireccionEmergencia = false;
    private boolean demoraPorRedireccionNormal = false;
    private final Semaphore ejecutar;
    private Vehiculo vehiculoActual;
    private int tiempoActualCabina;
    private int cantBloqueosEmergencia;    
    private int cantBloqueosNormal;     
    private Vehiculo normalesEnEspera[] = new Vehiculo[capacidadVia];
    private final Vehiculo emergenciasEnEspera[] = new Vehiculo[capacidadVia];
    
    public Cabina(Semaphore ejecutar){
        this.ejecutar = ejecutar;
        this.cantBloqueosEmergencia = 0;
        this.cantBloqueosNormal = 0;
    }

    @Override
    public void run() {                
        while(true){
            try {
                this.ejecutar.acquire(); 
                if(Planificador.finalizador){
                    break;
                }
                if(this.vehiculoActual != null){
                    this.procesarVehiculoActual();
                }
                Peaje.esperar.release();
            } catch (InterruptedException ex) {
                Logger.getLogger(Cabina.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void procesarVehiculoActual() throws InterruptedException{
        if(tiempoActualCabina==1){
            this.vehiculoActual = null;            
        }
        this.tiempoActualCabina--;
    }
    
    public void tomarVehiculo(){
        if(this.vehiculoActual == null){            
            if(this.emergenciasEnEspera[0] != null){                
                this.vehiculoActual = this.emergenciasEnEspera[0];
                this.emergenciasEnEspera[0]=null;
                this.cantEmergenciasEspera--;
            }else if(this.cantEmergenciasEspera == 0 && 
                this.normalesEnEspera[0] != null){                
                this.vehiculoActual = this.normalesEnEspera[0];
                this.normalesEnEspera[0]=null;
                this.cantNormalesEspera--;                
            }
            this.tiempoActualCabina = Peaje.DEMORA_CABINAS;
        }
    }
    
    public void avanzarVehiculos(Cabina.tipoVehiculo tipoVehiculo){        
        Vehiculo vehiculosEnEspera[] = null;
        int cantBloqueos = 0;
        
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            vehiculosEnEspera = this.emergenciasEnEspera;
            cantBloqueos = this.cantBloqueosEmergencia;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            vehiculosEnEspera = this.normalesEnEspera;
            cantBloqueos = this.cantBloqueosNormal;
        }
        
        for(int i=1;i<this.capacidadVia;i++){
            if(vehiculosEnEspera[i] != null){
                if(vehiculosEnEspera[i].getEstaBloqueando()){
                   vehiculosEnEspera[i].restarTiempoBloqueo();
                   if(!vehiculosEnEspera[i].getEstaBloqueando()){
                        cantBloqueos--;
                   }
                }
                else if(!vehiculosEnEspera[i].getFuiDistribuido()){
                    if(vehiculosEnEspera[i-1] == null){
                        vehiculosEnEspera[i-1] = vehiculosEnEspera[i];
                        vehiculosEnEspera[i] = null;
                        if(vehiculosEnEspera[i-1].bloquea(i-1)){
                            cantBloqueos++;                    
                            vehiculosEnEspera[i-1].setEstaBloqueando();
                        }
                    }
                }
                else{
                    vehiculosEnEspera[i].setFuiDistribuido(false);                    
                }                
            }
        }
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){            
            this.cantBloqueosEmergencia = cantBloqueos;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){            
            this.cantBloqueosNormal = cantBloqueos;
        }
    }  
    
    public void asignarVehiculo(Vehiculo vehiculo,Cabina.tipoVehiculo tipoVehiculo ){
        int ultimaPosicion = this.capacidadVia-1;
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            this.emergenciasEnEspera[ultimaPosicion] = vehiculo;
            this.cantEmergenciasEspera++;
            if(vehiculo.bloquea(ultimaPosicion)){
                this.cantBloqueosEmergencia++;                    
                this.emergenciasEnEspera[ultimaPosicion].setEstaBloqueando();
            }
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            this.normalesEnEspera[ultimaPosicion] = vehiculo;
            this.cantNormalesEspera++;            
            if(vehiculo.bloquea(ultimaPosicion)){
                this.cantBloqueosNormal++;      
                this.normalesEnEspera[ultimaPosicion].setEstaBloqueando();
            }
        }
        
    }
    
    public int getCantVehiculos(Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){        
            return this.cantEmergenciasEspera;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return this.cantNormalesEspera;
        }
        return 0;
    }
    
    public boolean aceptaVehiculos(Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){        
            return this.emergenciasEnEspera[this.capacidadVia-1] == null;
        }        
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return this.normalesEnEspera[this.capacidadVia-1] == null;        
        }
        return false;
    }
    
    public boolean estaBloqueada(Cabina.tipoVehiculo tipoVehiculo) {
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            return cantBloqueosEmergencia > 0;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return cantBloqueosNormal > 0;
        }
        return false;
    }
    
    public Vehiculo[] getVehiculos(Cabina.tipoVehiculo tipoVehiculo) {
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){        
            return emergenciasEnEspera;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return normalesEnEspera;
        }
        return null;
    }   
    
    public boolean estaVaciaPosicionVehiculo(int indice,Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            return this.emergenciasEnEspera[indice] == null;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return this.normalesEnEspera[indice] == null;
        }
        return false;
    }
    
    public void setPosicionVehiculo(int indice, Vehiculo vehiculo, Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){        
            this.emergenciasEnEspera[indice] = vehiculo;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            this.normalesEnEspera[indice] = vehiculo;
        }        
    }
    
    public void addCantVehiculosEnEspera(Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){        
            this.cantEmergenciasEspera++;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            this.cantNormalesEspera++;
        }        
    }
    
    public void decCantVehiculosEnEspera(Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){        
            this.cantEmergenciasEspera--;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            this.cantNormalesEspera--;
        }        
    }    
       
    public boolean estaVacia() {
        return this.vehiculoActual == null && 
                this.cantNormalesEspera == 0 &&
                    this.cantEmergenciasEspera == 0;
    }
    
    void ejecutar() {
        this.ejecutar.release();
    }
    
    public Vehiculo getVehiculoActual() {
        return this.vehiculoActual;
    }    
    
    public int getTiempoCabinaActual(){
        return this.tiempoActualCabina;
    }
    
    public Vehiculo getProximaEmergencia(){
        Vehiculo proximaEmergencia = null;
        for(int i=0;i<this.capacidadVia;i++){            
            if(this.emergenciasEnEspera[i] != null){
                proximaEmergencia = this.emergenciasEnEspera[i];
            }
        }
        return proximaEmergencia;
    }
    
    //DE ACA 
    public int vehiculosTrancados(Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            return cantEmergenciasEspera - cantBloqueosEmergencia;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return cantNormalesEspera - cantBloqueosNormal;
        }
        return 0;
    }
    
    public boolean estaDemorada(Cabina.tipoVehiculo tipoVehiculo){
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            return this.demoraPorRedireccionEmergencia;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            return this.demoraPorRedireccionNormal;
        }
        return false;
    }
    
    public void setDemorarCabina(boolean estado,Cabina.tipoVehiculo tipoVehiculo){        
        if(tipoVehiculo == Cabina.tipoVehiculo.EMERGENCIA){
            this.demoraPorRedireccionEmergencia = estado;
        }
        else if(tipoVehiculo == Cabina.tipoVehiculo.NORMAL){
            this.demoraPorRedireccionNormal= estado;
        }        
    }
    
    public int getCapacidadVia(){
        return this.capacidadVia;
    }
    //ACA
    
    public boolean primerNormalOcupado(){        
        return this.normalesEnEspera[0] !=null;
    } 
    
    static enum tipoVehiculo{
        EMERGENCIA,
        NORMAL;        
    }
}