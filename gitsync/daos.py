from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class AbstractMetadataDAO(Base):
    __abstract__ = True

    subject = Column(String(255), primary_key=True)
    source = Column(String(255), primary_key=True)
    policy = Column(Text)
    name = Column(String(255))
    ticker = Column(String(32))
    url = Column(String(255))
    description = Column(Text)
    decimals = Column(Integer)
    updated = Column(DateTime)
    updated_by = Column(String(255))
    properties = Column(JSONB)


class AbstractLogoDAO(Base):
    __abstract__ = True

    subject = Column(String(255), primary_key=True)
    source = Column(String(255), primary_key=True)
    logo = Column(Text)


class SyncControlDAO(Base):
    __tablename__ = 'sync_control'

    lock = Column(String(1), primary_key=True)
    registry_hash = Column(String(64))
    updated = Column(DateTime)


def get_metadata_table_model_instance(tablename: str):
    return type(f'MetadataDAO_{tablename}', (AbstractMetadataDAO,), {'__tablename__': tablename})

def get_logo_table_model_instance(tablename: str):
    return type(f'LogoDAO_{tablename}', (AbstractLogoDAO,), {'__tablename__': tablename})
