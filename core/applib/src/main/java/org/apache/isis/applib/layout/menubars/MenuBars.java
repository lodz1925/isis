/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.applib.layout.menubars;

import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.layout.component.ServiceActionLayoutData;

public interface MenuBars {

    @Programmatic
    String getTnsAndSchemaLocation();

    @Programmatic
    void setTnsAndSchemaLocation(final String tnsAndSchemaLocation);

    @Programmatic
    MenuBar menuBarFor(DomainServiceLayout.MenuBar menuBar);

    interface Visitor {
        void preVisit(final MenuBar menuBar);
        void visit(final MenuBar menuBar);
        void postVisit(final MenuBar menuBar);

        void preVisit(final Menu menu);
        void visit(final Menu menu);
        void postVisit(final Menu menu);

        void preVisit(final MenuSection menuSection);
        void visit(final MenuSection section);
        void postVisit(final MenuSection menuSection);

        void visit(final ServiceActionLayoutData serviceActionLayoutData);

    }

    class VisitorAdapter implements MenuBars.Visitor {
        @Override public void preVisit(final MenuBar menuBar) { }
        @Override public void visit(final MenuBar menuBar) { }
        @Override public void postVisit(final MenuBar menuBar) { }

        @Override public void preVisit(final Menu menu) { }
        @Override public void visit(final Menu menu) { }
        @Override public void postVisit(final Menu menu) { }

        @Override public void preVisit(final MenuSection menuSection) { }
        @Override public void visit(final MenuSection section) { }
        @Override public void postVisit(final MenuSection menuSection) { }

        @Override public void visit(final ServiceActionLayoutData serviceActionLayoutData) { }
    }

    @Programmatic
    void visit(final MenuBars.Visitor visitor);

}