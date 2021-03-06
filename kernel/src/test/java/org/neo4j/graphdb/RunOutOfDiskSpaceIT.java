/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphdb;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.io.IOException;

import javax.transaction.SystemException;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.mockfs.LimitedFileSystemGraphDatabase;
import org.neo4j.test.TargetDirectory;

public class RunOutOfDiskSpaceIT
{

    public TargetDirectory targetDir = TargetDirectory.forTest( RunOutOfDiskSpaceIT.class );

    @Rule
    public TargetDirectory.TestDirectory testDir = targetDir.cleanTestDirectory();

    @Test
    public void shouldPropagateIOExceptions() throws Exception
    {
        // Given
        TransactionFailureException exceptionThrown = null;
        LimitedFileSystemGraphDatabase db = new LimitedFileSystemGraphDatabase( testDir.directory().getAbsolutePath() );

        db.runOutOfDiskSpaceNao();

        // When
        Transaction tx = db.beginTx();
        db.createNode();
        tx.success();

        try {
            tx.finish();
            fail("Expected tx finish to throw TransactionFailureException when filesystem is full.");
        } catch(TransactionFailureException e)
        {
            exceptionThrown = e;
        }

        // Then
        assertThat(exceptionThrown.getCause(), is( Throwable.class ));
        assertThat(exceptionThrown.getCause().getCause(), is( Throwable.class ));
        assertThat(exceptionThrown.getCause().getCause().getCause(), is( IOException.class ));

    }

    @Test
    public void shouldStopDatabaseWhenOutOfDiskSpace() throws Exception
    {
        // Given
        TransactionFailureException errorCaught = null;
        LimitedFileSystemGraphDatabase db = new LimitedFileSystemGraphDatabase( testDir.directory().getAbsolutePath() );

        db.runOutOfDiskSpaceNao();

        Transaction tx = db.beginTx();
        db.createNode();
        tx.success();

        try {
            tx.finish();
            fail("Expected tx finish to throw TransactionFailureException when filesystem is full.");
        } catch(TransactionFailureException e)
        {
            // Expected
        }

        // When
        try {
            db.beginTx();
            fail( "Expected tx begin to throw TransactionFailureException when tx manager breaks." );
        } catch(TransactionFailureException e)
        {
            errorCaught = e;
        }

        // Then
        assertThat( errorCaught.getCause(), is( SystemException.class ) );

    }

}
