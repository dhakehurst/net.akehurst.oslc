/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.oslc3

/**
 * Not part of the OSLC Standard, defined here https://jazz.net/wiki/bin/view/Main/RootServicesSpec#Change_Management_Service_Provid
 */
object RootservicesConstants {

    const val xmlns_jfs = "http://jazz.net/xmlns/prod/jazz/jfs/1.0/"
    const val xmlns_jfs_abrv = "jfs"

    const val oauth_RequestTokenUrl: String = "oauthRequestTokenUrl"
    const val oauth_UserAuthorizationUrl: String = "oauthUserAuthorizationUrl"
    const val oauth_AccessTokenUrl: String = "oauthAccessTokenUrl"
    const val oauth_RealmName: String = "oauthRealmName"
    const val oauth_Domain: String = "oauthDomain"

    const val cm_ServiceProviderCatalog: String = "cmServiceProviders"
    const val qm_ServiceProviderCatalog: String = "qmServiceProviders"
    const val rm_ServiceProviderCatalog: String = "rmServiceProviders"
    const val am_ServiceProviderCatalog: String = "amServiceProviders"


}