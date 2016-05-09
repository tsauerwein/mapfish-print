package org.mapfish.print.map.tiled;

import com.codahale.metrics.MetricRegistry;
import jsr166y.ForkJoinPool;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;


import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class to support implementing layers that consist of Raster tiles which are combined to compose a single raster
 * to be drawn on the map.
 *
 * @author Jesse on 4/3/14.
 */
public abstract class AbstractTiledLayer extends AbstractGeotoolsLayer {


    private final StyleSupplier<GridCoverage2D> styleSupplier;
    private final ForkJoinPool forkJoinPool;

    /**
     * Registry.
     */
    protected final MetricRegistry registry;

    /**
     * Constructor.
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param styleSupplier strategy for loading the style for this layer
     * @param params the parameters for this layer
     * @param registry Metrics registry.
     */
    protected AbstractTiledLayer(final ForkJoinPool forkJoinPool,
                                 final StyleSupplier<GridCoverage2D> styleSupplier,
                                 final AbstractLayerParams params,
                                 final MetricRegistry registry) {
        super(forkJoinPool, params);
        this.forkJoinPool = forkJoinPool;
        this.styleSupplier = styleSupplier;
        this.registry = registry;
    }

    @Override
    protected final List<? extends Layer> getLayers(final MfClientHttpRequestFactory httpRequestFactory,
                                                    final MapfishMapContext mapContext,
                                                    final boolean isFirstLayer) throws Exception {
        double dpi = mapContext.getDPI();
        MapBounds bounds = mapContext.getBounds();
        Rectangle paintArea = new Rectangle(mapContext.getMapSize());
        TileCacheInformation tileCacheInformation = createTileInformation(bounds, paintArea, dpi, isFirstLayer);
        final TileLoaderTask task = new TileLoaderTask(httpRequestFactory, dpi,
                mapContext, tileCacheInformation, getFailOnError(), this.registry);
        final GridCoverage2D gridCoverage2D = this.forkJoinPool.invoke(task);

        GridCoverageLayer layer = new GridCoverageLayer(gridCoverage2D, this.styleSupplier.load(httpRequestFactory, gridCoverage2D,
                mapContext));
        return Collections.singletonList(layer);
    }

    /**
     * Create the tile cache information object for the given parameters.
     *
     * @param bounds    the map bounds
     * @param paintArea the area to paint
     * @param dpi       the DPI to render at
     * @param isFirstLayer true indicates this layer is the first layer in the map (the first layer drawn, ie the base layer)
     */
    protected abstract TileCacheInformation createTileInformation(MapBounds bounds, Rectangle paintArea, double dpi,
                                                                  final boolean isFirstLayer);

}
