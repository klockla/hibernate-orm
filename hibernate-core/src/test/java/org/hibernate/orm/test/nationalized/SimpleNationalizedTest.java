/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.nationalized;

import java.sql.NClob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.CharacterArrayType;
import org.hibernate.type.CharacterNCharType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.MaterializedNClobType;
import org.hibernate.type.NClobType;
import org.hibernate.type.NTextType;
import org.hibernate.type.StringNVarcharType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NVarcharTypeDescriptor;
import org.hibernate.type.internal.StandardBasicTypeImpl;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class SimpleNationalizedTest {

	@SuppressWarnings({ "UnusedDeclaration", "SpellCheckingInspection" })
	@Entity(name = "NationalizedEntity")
	public static class NationalizedEntity {
		@Id
		private Integer id;

		@Nationalized
		private String nvarcharAtt;

		@Lob
		@Nationalized
		private String materializedNclobAtt;

		@Lob
		@Nationalized
		private NClob nclobAtt;

		@Nationalized
		private Character ncharacterAtt;

		@Nationalized
		private Character[] ncharArrAtt;

		@Type(type = "ntext")
		private String nlongvarcharcharAtt;
	}

	@Test
	public void simpleNationalizedTest() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedEntity.class );

			final Metadata metadata = ms.buildMetadata();
			PersistentClass pc = metadata.getEntityBinding( NationalizedEntity.class.getName() );
			assertNotNull( pc );

			Property prop = pc.getProperty( "nvarcharAtt" );
			final Dialect dialect = metadata.getDatabase().getDialect();
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				assertSame( StringType.INSTANCE, prop.getType() );
			}
			else {
				assertSame( StringNVarcharType.INSTANCE, prop.getType() );
			}

			prop = pc.getProperty( "materializedNclobAtt" );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				if ( dialect instanceof SybaseDialect ) {
					assertThat( prop.getType(), instanceOf( StandardBasicTypeImpl.class ) );
					final StandardBasicTypeImpl type = (StandardBasicTypeImpl) prop.getType();
					assertSame( StringTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
					assertSame( ClobTypeDescriptor.CLOB_BINDING, type.getJdbcTypeDescriptor() );
				}
				else {
					assertSame( MaterializedClobType.INSTANCE, prop.getType() );
				}

			}
			else {
				assertSame( MaterializedNClobType.INSTANCE, prop.getType() );
			}
			prop = pc.getProperty( "nclobAtt" );
			assertSame( NClobType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			assertSame( NTextType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "ncharArrAtt" );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				assertSame( CharacterArrayType.INSTANCE, prop.getType() );
			}
			else {
				assertThat( prop.getType(), instanceOf( StandardBasicTypeImpl.class ) );
				StandardBasicTypeImpl type = (StandardBasicTypeImpl) prop.getType();
				assertThat( type.getJavaTypeDescriptor(), instanceOf( CharacterArrayTypeDescriptor.class ) );
				assertThat( type.getJdbcTypeDescriptor(), instanceOf( NVarcharTypeDescriptor.class ) );
			}
			prop = pc.getProperty( "ncharacterAtt" );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				assertSame( CharacterType.INSTANCE, prop.getType() );
			}
			else {
				assertSame( CharacterNCharType.INSTANCE, prop.getType() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
