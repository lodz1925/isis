package org.nakedobjects.object.io;

import org.nakedobjects.NakedObjects;
import org.nakedobjects.object.InternalCollection;
import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectRuntimeException;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.persistence.Oid;
import org.nakedobjects.object.reflect.NakedObjectField;
import org.nakedobjects.object.reflect.OneToManyAssociation;
import org.nakedobjects.object.reflect.OneToOneAssociation;
import org.nakedobjects.object.reflect.PojoAdapterFactory;

import java.io.Serializable;
import java.util.Vector;

import org.apache.log4j.Logger;



public class Memento implements Transferable, Serializable {
    private final static long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(Memento.class);
    private Data state;

    /**
     * Creates a memento that hold the state for the specified object. This
     * object is Serializable and can be passed over the network easily. Also
     * for a persistent only the refernce Oids are held, avoiding the need for
     * serializing the whole object graph.
     */
    public Memento(NakedObject object) {
        state = object == null ? null : createData(object);
        LOG.debug("created memento for " + this);
    }

    public Memento() {}

    private Data createData(Naked object) {
        if (object instanceof InternalCollection) {
            InternalCollection coll = (InternalCollection) object;
            Data[] collData = new Data[coll.size()];
            for (int j = 0; j < coll.size(); j++) {
                NakedObject ref = coll.elementAt(j);
                collData[j] = new Data(ref.getOid(), ref.getSpecification().getFullName());
            }

            return new InternalCollectionData(coll.getOid(), InternalCollection.class.getName(), collData);
        } else {
            return createObjectData((NakedObject) object);
        }
    }

    private ObjectData createObjectData(NakedObject object) {
        NakedObjectSpecification cls = object.getSpecification();
        ObjectData d = new ObjectData(object.getOid(), cls.getFullName());

        NakedObjectField[] fields = cls.getFields();

        for (int i = 0; i < fields.length; i++) {
            NakedObjectField field = fields[i];
            if (!field.isDerived()) {
               if (field instanceof OneToManyAssociation) {
                    InternalCollection coll = (InternalCollection) object.getField(field);
                    d.addField(field.getName(), createData(coll));
                } else if (field instanceof OneToOneAssociation) {
                    NakedObject ref = object.getAssociation((OneToOneAssociation) field);
                    Object refOid = ref == null ? null : new Data(ref.getOid(), ref.getSpecification().getFullName());
                    d.addField(field.getName(), refOid);
                }
            }
        }
        return d;
    }

    public Oid getOid() {
        return state.oid;
    }

    public NakedObject recreateObject() {
        if (state == null) {
            return null;
        } else {
            NakedObjectSpecification spec = NakedObjects.getSpecificationLoader().loadSpecification(state.className);
            PojoAdapterFactory objectLoader = NakedObjects.getPojoAdapterFactory();
            NakedObject object;
            if(getOid() == null) {
                object = objectLoader.createTransientInstance(spec);
            } else {
                object = objectLoader.recreateAdapter(getOid(), spec);
            }
            
            LOG.debug("Recreated object " + object.getOid());
            updateObject(object);

            return object;
        }
    }

    private NakedObject recreateReference(Data data) {
        PojoAdapterFactory objectLoader = NakedObjects.getPojoAdapterFactory();
        synchronized (objectLoader) {
            Oid oid = data.oid;

            NakedObject ref;
            if (oid == null) {
                ref = null;
            } else {
	            NakedObjectSpecification spec = NakedObjects.getSpecificationLoader().loadSpecification(data.className);
                ref = objectLoader.recreateAdapter(oid, spec);
            }
            return ref;
        }
    }

    public String toString() {
        return "[" + (state == null ? null : state.className + "/" + state.oid + state) + "]";
    }

    /**
     * Updates the specified object (assuming it is the correct object for this
     * memento) with the state held by this memento.
     * 
     * @throws IllegalArgumentException
     *                       if the memento was created from different logical object to
     *                       the one specified (i.e. its oid differs).
     */
    public void updateObject(NakedObject object) {
        Object oid = object.getOid();
        if (oid != null && !oid.equals(state.oid)) {
            throw new IllegalArgumentException("This memento can only be used to " + "update the naked object with the Oid "
                    + state.oid);

        } else {
            if (!(state instanceof ObjectData)) {
                throw new NakedObjectRuntimeException("Expected an ObjectData but got " + state.getClass());
            } else {
                PojoAdapterFactory objectLoader = NakedObjects.getPojoAdapterFactory();

                objectLoader.loading(object, true);
                
                ObjectData od = (ObjectData) state;

                NakedObjectField[] fields = object.getSpecification().getFields();

                for (int i = 0; i < fields.length; i++) {
                    NakedObjectField field = fields[i];
                    Object fieldData = od.getEntry(field.getName());
                    if (!field.isDerived()) {
                        if (field instanceof OneToManyAssociation) {
                            updateOneToManyAssociation(object, (OneToManyAssociation) field,
                                    (InternalCollectionData) fieldData);
                        } else if (field instanceof OneToOneAssociation) {
                            updateOneToOneAssociation(object, (OneToOneAssociation) field, (Data) fieldData);
                        }
                    }
                }
                
                objectLoader.loaded(object, true);
            }
            LOG.debug("object updated " + object.getOid());
        }

    }

    private void updateOneToManyAssociation(NakedObject object, OneToManyAssociation field, InternalCollectionData collectionData) {
        InternalCollection collection = (InternalCollection) object.getField(field);
        if (collection.getOid() == null) {
            collection.setOid(collectionData.getOid());
        }

        Vector original = new Vector();
        int size = collection.size();
        for (int i = 0; i < size; i++) {
            original.addElement(collection.elementAt(i));
        }

        for (int j = 0; j < collectionData.elements.length; j++) {
            NakedObject element = recreateReference((Data) collectionData.elements[j]);
            if (!collection.contains(element)) {
                LOG.debug("  association " + field + " changed, added " + element.getOid());
                object.setAssociation(field, element);
            } else {
                object.clearAssociation(field, element);
            }
        }

        size = original.size();
        for (int i = 0; i < size; i++) {
            NakedObject element = (NakedObject) original.elementAt(i);
            LOG.debug("  association " + field + " changed, removed " + element.getOid());
            object.clearAssociation(field, element);
        }
    }

    private void updateOneToOneAssociation(NakedObject object, OneToOneAssociation field, Data fieldData) {
        if (fieldData == null) {
            object.setValue(field, null);
        } else {
            NakedObject ref = recreateReference(fieldData);
            if (object.getField(field) != ref) {
                LOG.debug("  association " + field + " changed to " + ref.getOid());
                object.setValue(field, ref);
            }
        }
    }

    public void writeData(TransferableWriter data) {
        data.writeObject(state);
    }

    public void restore(TransferableReader data) {
        state = (Data) data.readObject();
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2005 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */