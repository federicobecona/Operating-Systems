package peaje;

public class Vehiculo {
    
    private final String matricula;
    private final int tiempoEntrada;
    private final boolean tienePrioridad;    
    private boolean estaBloqueando;
    private boolean fuiDistribuido;
    private final int posicionBloqueo;
    private int tiempoBloqueo;
    
    
    public Vehiculo(String matricula, int tiempoEntrada, int prioridad,int posicionBloqueo, int tiempoBloqueo){
        this.matricula = matricula;
        this.tiempoEntrada = tiempoEntrada; 
        this.tienePrioridad = prioridad==1;        
        this.estaBloqueando = false;
        this.posicionBloqueo = posicionBloqueo;
        this.tiempoBloqueo = tiempoBloqueo;        
    }
       
    public int getTiempoEntrada() {
        return tiempoEntrada;
    }
    
    public String getMatricula(){
        return this.matricula;
    }
    
    public boolean getTienePrioridad(){
        return this.tienePrioridad;
    }
    
    public boolean bloquea(int posicion) {
        return posicion == this.posicionBloqueo && tiempoBloqueo > 0;
    }
    
    public int getPosicionBloqueo() {
        return posicionBloqueo;
    }
    
    public int getTiempoBloqueo() {
        return tiempoBloqueo;
    }
    
    public void restarTiempoBloqueo(){
        this.tiempoBloqueo--;
        if(this.tiempoBloqueo <= 0){
            this.estaBloqueando = false;
        }
    }

    public boolean getEstaBloqueando() {
        return estaBloqueando;
    }

    public void setEstaBloqueando() {
        this.estaBloqueando = true;
    }    

    public boolean getFuiDistribuido() {
        return fuiDistribuido;
    }

    public void setFuiDistribuido(boolean fuiDistribuido) {
        this.fuiDistribuido = fuiDistribuido;
    }

}
