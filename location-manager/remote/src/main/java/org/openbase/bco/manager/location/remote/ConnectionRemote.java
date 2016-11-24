package org.openbase.bco.manager.location.remote;

/*
 * #%L
 * COMA LocationManager Remote
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.manager.location.lib.Connection;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.DoorStateType.DoorState;
import rst.domotic.state.HandleStateType.HandleState;
import rst.domotic.state.PassageStateType.PassageState;
import rst.domotic.state.WindowStateType.WindowState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @deprecated please use org.openbase.bco.dal.remote.unit.connection.ConnectionRemote
 */
public class ConnectionRemote extends org.openbase.bco.dal.remote.unit.connection.ConnectionRemote {

}
