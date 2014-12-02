/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.operation;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.storage.FeatureInserter;
import org.locationtech.udig.processingtoolbox.storage.IFeatureInserter;
import org.locationtech.udig.processingtoolbox.storage.MemoryDataStore2;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Abstract General Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(GeneralOperation.class);

    protected final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private String outputTypeName = null;

    private DataStore outputDataStore = null;

    public void setOutputDataStore(DataStore outputDataStore) {
        this.outputDataStore = outputDataStore;
    }

    public DataStore getOutputDataStore() {
        if (outputDataStore == null) {
            outputDataStore = new MemoryDataStore2();
        }
        return outputDataStore;
    }

    public void setOutputTypeName(String outputTypeName) {
        this.outputTypeName = outputTypeName;
    }

    @SuppressWarnings("nls")
    public String getOutputTypeName() {
        if (outputTypeName == null) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd_hhmmss_S");
            String serialID = dataFormat.format(Calendar.getInstance().getTime());
            outputTypeName = "gp_" + serialID;
        }

        return outputTypeName;
    }

    protected boolean isShapefileDataStore(DataStore dataStore) {
        if (dataStore instanceof DirectoryDataStore) {
            return true;
        } else if (dataStore instanceof ShapefileDataStore) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("nls")
    protected IFeatureInserter getFeatureWriter(SimpleFeatureType schema) throws IOException {
        // create schema
        SimpleFeatureStore featureStore = null;
        getOutputDataStore().createSchema(schema);

        final String typeName = schema.getTypeName();
        SimpleFeatureSource featureSource = getOutputDataStore().getFeatureSource(typeName);
        if (featureSource instanceof SimpleFeatureStore) {
            featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(new DefaultTransaction(typeName));
        } else {
            LOGGER.log(Level.WARNING, typeName + " does not support SimpleFeatureStore interface!");
            featureStore = (SimpleFeatureStore) featureSource;
        }

        return new FeatureInserter(featureStore);
    }

    protected void insertFeature(IFeatureInserter featureWriter, SimpleFeature origFeature,
            Geometry geometry) throws IOException {
        SimpleFeature newFeature = featureWriter.buildFeature(null);
        featureWriter.copyAttributes(origFeature, newFeature, false);
        newFeature.setDefaultGeometry(geometry);

        featureWriter.write(newFeature);
    }
}