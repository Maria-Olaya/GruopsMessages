package eafit.gruopChat.shared.enums;

public enum MessageStatus {
    SENT,       // Servidor recibió y guardó
    DELIVERED,  // Llegó al dispositivo de al menos un destinatario
    READ        // Todos los miembros del grupo/canal lo leyeron
}