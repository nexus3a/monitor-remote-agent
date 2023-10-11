/*
 * Copyright 2023 Алексей.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.monitor.parser.onec;

import java.util.HashMap;

/**
 *
 * @author Алексей
 */
public class OneCTJRecord extends HashMap<String, Object> {
    protected boolean containsLocks = false;
    protected boolean escalating = false;

    @Override
    public void clear() {
        super.clear();
        containsLocks = false;
        escalating = false;
    }
}
