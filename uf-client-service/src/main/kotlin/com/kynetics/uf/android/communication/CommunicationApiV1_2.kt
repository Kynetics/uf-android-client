/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.communication

import com.kynetics.uf.android.api.TargetAttributesWithPolicy

@Suppress("ClassName")
interface CommunicationApiV1_2:CommunicationApiV1_1{
    fun addTargetAttributes(targetAttributesWithPolicy: TargetAttributesWithPolicy)
}