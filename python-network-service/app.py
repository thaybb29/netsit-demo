from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import subprocess
import xml.etree.ElementTree as ET
import os
import platform
import ipaddress
import re
from typing import Optional


app = FastAPI(
    title="NetSit Network Service",
    version="1.0.0"
)


# ============================================================
# CONFIGURACIÓN
# ============================================================

NMAP_PATH = os.getenv("NMAP_PATH", "nmap")


# ============================================================
# MODELOS
# ============================================================

class ScanRequest(BaseModel):
    subnet: str


class PingRequest(BaseModel):
    ip: str


# ============================================================
# FUNCIONES AUXILIARES
# ============================================================

def obtener_nmap():
    """
    Busca Nmap en el PATH del sistema.
    En Windows también intenta ubicaciones comunes.
    """

    posibles = [
        NMAP_PATH,
        r"C:\Program Files\Nmap\nmap.exe",
        r"C:\Program Files (x86)\Nmap\nmap.exe"
    ]

    for ruta in posibles:

        try:
            resultado = subprocess.run(
                [ruta, "--version"],
                capture_output=True,
                text=True,
                timeout=10
            )

            if resultado.returncode == 0:
                return ruta

        except Exception:
            pass

    return None


def validar_subred(subnet: str):

    try:
        red = ipaddress.ip_network(subnet, strict=False)

        # Evitamos accidentalmente escaneos enormes.
        if red.num_addresses > 65536:
            raise ValueError(
                "La red es demasiado grande. "
                "Utiliza una subred como 192.168.1.0/24."
            )

        return red

    except Exception as e:
        raise ValueError(f"Subred inválida: {e}")


def limpiar_hostname(hostname: Optional[str]):

    if not hostname:
        return None

    hostname = hostname.strip()

    if not hostname:
        return None

    return hostname


def ejecutar_nmap(subnet: str):

    nmap = obtener_nmap()

    if not nmap:
        raise RuntimeError(
            "Nmap no está instalado o no se encuentra en el PATH."
        )

    validar_subred(subnet)

    comando = [
        nmap,
        "-sn",
        "-oX",
        "-",
        subnet
    ]

    try:

        resultado = subprocess.run(
            comando,
            capture_output=True,
            text=True,
            timeout=120
        )

    except subprocess.TimeoutExpired:

        raise RuntimeError(
            "El escaneo de Nmap tardó demasiado."
        )

    except Exception as e:

        raise RuntimeError(
            f"No se pudo ejecutar Nmap: {e}"
        )

    if resultado.returncode != 0:

        mensaje = resultado.stderr.strip()

        raise RuntimeError(
            mensaje or "Nmap terminó con un error."
        )

    return resultado.stdout


def analizar_resultado_nmap(xml_text: str):

    dispositivos = []

    try:

        root = ET.fromstring(xml_text)

    except ET.ParseError as e:

        raise RuntimeError(
            f"No se pudo interpretar la respuesta de Nmap: {e}"
        )

    for host in root.findall("host"):

        estado_elemento = host.find("status")

        if estado_elemento is None:
            continue

        estado = estado_elemento.attrib.get("state")

        if estado != "up":
            continue

        ip = None
        mac = None
        hostname = None
        fabricante = None

        # ----------------------------------------------------
        # DIRECCIONES
        # ----------------------------------------------------

        for address in host.findall("address"):

            addr_type = address.attrib.get("addrtype")
            addr = address.attrib.get("addr")

            if addr_type == "ipv4":
                ip = addr

            elif addr_type == "mac":

                mac = addr

                fabricante = address.attrib.get(
                    "vendor"
                )

        # ----------------------------------------------------
        # HOSTNAME
        # ----------------------------------------------------

        hostnames = host.find("hostnames")

        if hostnames is not None:

            hostname_element = hostnames.find("hostname")

            if hostname_element is not None:

                hostname = hostname_element.attrib.get(
                    "name"
                )

        hostname = limpiar_hostname(hostname)

        # ----------------------------------------------------
        # VALIDACIÓN
        # ----------------------------------------------------

        if not ip:
            continue

        dispositivos.append({
            "ip": ip,
            "mac": mac,
            "hostname": hostname,
            "fabricante": fabricante,
            "activo": True
        })

    return dispositivos


# ============================================================
# ENDPOINT HEALTH
# ============================================================

@app.get("/health")
def health():

    nmap = obtener_nmap()

    return {
        "status": "UP",
        "python": platform.python_version(),
        "sistema": platform.system(),
        "nmap": nmap is not None,
        "nmapPath": nmap
    }


# ============================================================
# ESCANEAR RED
# ============================================================

@app.post("/api/network/scan")
def scan_network(request: ScanRequest):

    try:

        validar_subred(request.subnet)

        xml = ejecutar_nmap(
            request.subnet
        )

        dispositivos = analizar_resultado_nmap(
            xml
        )

        return {
            "subnet": request.subnet,
            "total": len(dispositivos),
            "devices": dispositivos
        }

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# ============================================================
# PING
# ============================================================

@app.post("/api/network/ping")
def ping(request: PingRequest):

    ip = request.ip.strip()

    if not ip:
        raise HTTPException(
            status_code=400,
            detail="Debe indicar una IP."
        )

    try:

        ipaddress.ip_address(ip)

    except ValueError:

        raise HTTPException(
            status_code=400,
            detail="La IP no es válida."
        )

    if platform.system().lower() == "windows":

        comando = [
            "ping",
            "-n",
            "1",
            "-w",
            "2000",
            ip
        ]

    else:

        comando = [
            "ping",
            "-c",
            "1",
            "-W",
            "2",
            ip
        ]

    try:

        resultado = subprocess.run(
            comando,
            capture_output=True,
            text=True,
            timeout=5
        )

        activo = resultado.returncode == 0

        return {
            "ip": ip,
            "activo": activo,
            "resultado": (
                resultado.stdout
                if resultado.stdout
                else resultado.stderr
            )
        }

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# ============================================================
# EJECUCIÓN DIRECTA
# ============================================================

if __name__ == "__main__":

    import uvicorn

    uvicorn.run(
        app,
        host="127.0.0.1",
        port=5000
    )