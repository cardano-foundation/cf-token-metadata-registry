from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class MetadataDAO(Base):
  __tablename__ = 'metadata'

  subject = Column(String(255), primary_key=True)
  policy = Column(Text)
  name = Column(String(255))
  ticker = Column(String(32))
  url = Column(String(255))
  description = Column(Text)
  decimals = Column(Integer)
  updated = Column(DateTime)
  updated_by = Column(String(255))
  properties = Column(JSONB)

class LogoDAO(Base):
  __tablename__ = 'logo'

  subject = Column(String(255), primary_key=True)
  logo = Column(Text)

class TempMetadataDAO(Base):
  __tablename__ = 'tmp_metadata'

  subject = Column(String(255), primary_key=True)
  policy = Column(Text)
  name = Column(String(255))
  ticker = Column(String(32))
  url = Column(String(255))
  description = Column(Text)
  decimals = Column(Integer)
  updated = Column(DateTime)
  updated_by = Column(String(255))
  properties = Column(JSONB)

class TempLogoDAO(Base):
  __tablename__ = 'tmp_logo'

  subject = Column(String(255), primary_key=True)
  logo = Column(Text)
