package com.silicio.copi.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.validation.HostValidator

/**
 * Punto de entrada para Android Auto / Android Automotive.
 * Categoría NAVIGATION declarada en AndroidManifest.xml.
 */
@ExperimentalCarApi
class CopiCarAppService : CarAppService() {
    /**
     * Para sideload/DHU y uso personal se aceptan todos los hosts. Una
     * publicación en Google Play debe usar HostValidator.Builder con la
     * allowlist de hosts y superar la revisión de la categoría Navegación.
     */
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = CopiSession()
}
